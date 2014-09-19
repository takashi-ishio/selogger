package selogger.weaver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;


public class Weaver {

	private File outputDir;
	private List<File> targetFiles;
	private WeavingInfo weavingInfo;
	private MessageDigest md5algorithm;
	
	
	/**
	 * 
	 * @param outputDir specifies a writable directory where Weaver outputs files.
	 */
	public Weaver(WeavingInfo w) {
		this.outputDir = w.getOutputDir();
		this.targetFiles = new ArrayList<File>();
		this.weavingInfo = w;
		try {
			this.md5algorithm = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			this.md5algorithm = null;
		}
	}
	
	/**
	 * Close files.
	 */
	public void close() {
		this.weavingInfo.close();
	}
	
	
	/**
	 * 
	 * @param f specifies a class file, a jar file, a zip file or a directory including class files.
	 * The file type is checked by the extension of the file name.
	 */
	public void addTarget(File f) {
		if (!isClassFile(f) && !isJarFile(f) && !isZipFile(f) && !f.isDirectory()) {
			throw new IllegalArgumentException("File argument (" + outputDir.getAbsolutePath() + ") must be a class file or a directory.");
		}
		this.targetFiles.add(f);
	}
	
	
	/**
	 * Weave logging code into target files.
	 */
	public void weave() {
		for (File f: targetFiles) {
			
			boolean success;
			if (isClassFile(f)) {
				success = weaveClassFile(f);
			} else if (isJarFile(f) || isZipFile(f)) {
				success = weaveJarFileImpl(f, new File(outputDir, f.getName()));
			} else if (f.isDirectory()) {
				success = weaveDirectory(f);
			} else {
				weavingInfo.log(f.getAbsolutePath() + " is not a class/jar file.");
				success = false;
			}
			
			if (!success && !weavingInfo.ignoreError()) return;
		}
	}
	
	/**
	 * @param weavingInfo 
	 * @param classFile
	 */
	public boolean weaveClassFile(File classFile) {
		try {
			FileInputStream in = new FileInputStream(classFile);
			byte[] target = ClassTransformer.streamToByteArray(in);
			in.close();

			boolean success = weaveClassImpl("", classFile.getAbsolutePath(), target, null);
			return success;
		} catch (IOException e) {
			weavingInfo.log(e);
			return false;
		}
	}
	
	/**
	 * Weave class files involved in a directory and its all sub-directories.
	 * @param dir specifies a directory. 
	 */
	public boolean weaveDirectory(File dir) {
		assertIsDirectory(dir);
		LinkedList<File> dirs = new LinkedList<File>();
		dirs.add(dir);
		
		while (!dirs.isEmpty()) {
			File d = dirs.removeFirst();
			File[] files = d.listFiles();
			for (File f: files) {
				if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
					dirs.add(f);
				} else if (isJarFile(f) && weavingInfo.weaveJarsInDir()) {
					String relativePath = f.getAbsolutePath().substring(dir.getAbsolutePath().length() + File.separator.length());
					File outJar = new File(outputDir, relativePath);
					weavingInfo.log("Weave: " + f.getAbsolutePath() + " -> " + outJar.getAbsolutePath());
					boolean success = weaveJarFileImpl(f, outJar);
					if (!success && !weavingInfo.ignoreError()) {
						return false;
					}
				} else if (isClassFile(f)) {
					boolean success = weaveClassFile(f);
					if (!success && !weavingInfo.ignoreError()) { 
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean weaveJarFileImpl(File inputJarFile, File outputJarFile) {
		FileInputStream injar = null; 
		FileOutputStream outjar = null;
		ZipInputStream injarStream = null;
		JarOutputStream outjarStream = null;
		try {
			injar = new FileInputStream(inputJarFile);
			injarStream = new ZipInputStream(injar);
			outjar = new FileOutputStream(outputJarFile);
			outjarStream = new JarOutputStream(outjar);
			
			boolean success = weaveClassesInJarFile(inputJarFile.getCanonicalPath(), injarStream, outjarStream);
			
			injarStream.close();
			outjarStream.close();
			return success;
		} catch (IOException e) {
			try {
				if (injarStream != null) injarStream.close();
				else if (injar != null) injar.close();
			} catch (IOException e2) {
			}
			try {
				if (outjarStream != null) outjarStream.close();
				else if (outjar != null) outjar.close();
			} catch (IOException e2) {
			}
			return false;
		}
		
	}
	

	private String getClassMD5(byte[] targetClass) {
		if (md5algorithm != null) {
			byte[] hash = md5algorithm.digest(targetClass);
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b: hash) {
				String l = "0" + Integer.toHexString(b);
				hex.append(l.substring(l.length() - 2));
			}
			return hex.toString();
		} else {
			return "";
		}
	}
	
	/**
	 * 
	 * @param container specifies a container file name.  This is recorded for reference.  
	 * @param filename specifies a file name.  This is also recorded for reference.
	 * @param target specifies a byte array including a class data.
	 * @param output specifies an output stream (e.g. a JAR output stream).  
	 *  If null, a class file is automatically created according to the package name and class name.
	 * @return
	 * @throws IOException
	 */
	private boolean weaveClassImpl(String container, String filename, byte[] target, OutputStream output) throws IOException {
		assert container != null;
		
		String md5 = getClassMD5(target);
		LogLevel level = LogLevel.Normal;
		try {
			ClassTransformer c;
			try {
				c = new ClassTransformer(weavingInfo, target);
			} catch (RuntimeException e) {
				if ("Method code too large!".equals(e.getMessage())) {
					// Retry to generate a smaller bytecode by ignoring a large array init block
					try {
						weavingInfo.rollbackLocationId();
						level = LogLevel.IgnoreArrayInitializer;
						c = new ClassTransformer(weavingInfo, target, level);
					    weavingInfo.log("LogLevel.IgnoreArrayInitializer: " + container + "/" + filename);
					} catch (RuntimeException e2) {
						if ("Method code too large!".equals(e.getMessage())) {
							// Retry to generate further smaller bytecode by ignoring except for entry and exit events
							weavingInfo.rollbackLocationId();
							level = LogLevel.OnlyEntryExit;
							c = new ClassTransformer(weavingInfo, target, level);
						    weavingInfo.log("LogLevel.OnlyEntryExit: " + container + "/" + filename);
						} else {
							throw e2;
						}
					}
				} else {
					throw e;
				}
			}
			
			// Write the result to a disk
			if (output != null) {
				output.write(c.getWeaveResult());
				weavingInfo.finishClassProcess(container, filename, c.getFullClassName(), level, md5);
				doVerification(filename, c.getWeaveResult());
				return true;
			} else {
				
				boolean hasPackageName = (c.getPackageName() != null);
				File outputPackageDir = outputDir;
				if (hasPackageName) {
					outputPackageDir = new File(outputDir, c.getPackageName()); 
				}
				 
				if (outputPackageDir.exists() || outputPackageDir.mkdirs()) {
					try {
						File outputFile;
						if (hasPackageName) {
							outputFile = new File(outputPackageDir, c.getClassName() + ".class"); 
						} else { 
							outputFile = new File(outputPackageDir, c.getFullClassName() + ".class"); 
						}
						FileOutputStream stream = new FileOutputStream(outputFile);
						stream.write(c.getWeaveResult());
						stream.close();
						weavingInfo.finishClassProcess(container, filename, c.getFullClassName(), level, md5);
						doVerification(filename, c.getWeaveResult());
						return true;
					} catch (IOException e) {
						weavingInfo.rollbackLocationId();
						return false;
					}
				} else {
					weavingInfo.rollbackLocationId();
					weavingInfo.log("Failed to create a package dir: " + outputPackageDir.getAbsolutePath());
					return false;
				}
			}
			
		} catch (RuntimeException e) { 
			weavingInfo.rollbackLocationId();
			if (container != null && container.length() > 0) {
				weavingInfo.log("Failed to weave " + filename + " in " + container);
			} else {
				weavingInfo.log("Failed to weave " + filename);
			}
			weavingInfo.log(e);
			weavingInfo.finishClassProcess(container, filename, "", LogLevel.Failed, md5);
			if (output != null) {
				output.write(target); // write the base bytecode
			} else {
				weavingInfo.log(filename + " is not copied to the output directory.");
			}
			return weavingInfo.ignoreError();
		}
	}
	
	/**
	 * Read fiels from inputJar, and then outputs woven classes to outputJar.
	 * This method is separated from the caller, because recursive calls.
	 * @param inputJar
	 * @param outputJar
	 */
	private boolean weaveClassesInJarFile(String inputjarName,  ZipInputStream inputJar, JarOutputStream outputJar) throws IOException {
		boolean success = true;
		for (ZipEntry entry = inputJar.getNextEntry(); entry != null; entry = inputJar.getNextEntry()) {  
			JarEntry outEntry = new JarEntry(entry.getName());
			outputJar.putNextEntry(outEntry);
			
			if (entry.getName().endsWith(".class")) { // entry is a class file
				byte[] target = ClassTransformer.streamToByteArray(inputJar);
				success = weaveClassImpl(inputjarName, entry.getName(), target, outputJar);
				
			} else if (entry.getName().endsWith(".jar") && weavingInfo.weaveInternalJAR()) {
				byte[] internalJar = ClassTransformer.streamToByteArray(inputJar);
				ByteArrayInputStream b = new ByteArrayInputStream(internalJar);
				ZipInputStream internalJarStream = new ZipInputStream(b);
				
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				JarOutputStream bufWriter = new JarOutputStream(buf);
				
				boolean suc = weaveClassesInJarFile(inputjarName + "/" + entry.getName(), internalJarStream, bufWriter);
				if (suc) {
					bufWriter.close();
					outputJar.write(buf.toByteArray());
				} else {
					outputJar.write(internalJar);
					success = false;
				}
			} else {
				byte[] target = ClassTransformer.streamToByteArray(inputJar);
				outputJar.write(target);
			}
			outputJar.closeEntry();
			
		}
		return success;
	}
	
	private boolean isClassFile(File f) {
		return f.getAbsolutePath().endsWith(".class");
	}
	
	private boolean isJarFile(File f) {
		return f.getAbsolutePath().endsWith(".jar");
	}
	
	private boolean isZipFile(File f) {
		return f.getAbsolutePath().endsWith(".zip");
	}
	
	private void assertIsDirectory(File dir) {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("File argument (" + dir.getAbsolutePath() + ") must be a a directory.");
		}
	}
	
	private void doVerification(String name, byte[] b) {
		if (weavingInfo.isVerifierEnabled()) {
		      StringWriter sw = new StringWriter();
		      PrintWriter pw = new PrintWriter(sw);
		      CheckClassAdapter.verify(new ClassReader(b), true, pw); // this method is not expected to throw an exception
		      weavingInfo.log("VERIFICATION " + name);
		      weavingInfo.log(sw.toString());
		}
	}
}
