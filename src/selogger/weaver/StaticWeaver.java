package selogger.weaver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;

/**
 * An implementation of Static Weaving.
 * Currently, this is just an optional feature to statically analyze 
 * SELogger's logging code, because the woven code has no way 
 * to select a Logger (e.g. nearomni/freq/discard). 
 */
public class StaticWeaver {

	/**
	 * @param args specify the weaving options and target files.
	 * The first argument is assumed as weaving options.
	 * The remaining arguments are class files, jar files, and directories.  
	 */
	public static void main(String[] args) {
		if (args.length == 0) return;
		
		StaticWeaver w = new StaticWeaver(args[0]);
		for (int i=1; i<args.length; i++) {
			w.weave(args[i]);
		}
	}
	
	/**
	 * Currently hard coded
	 */
	private boolean weaveInternalJar = false;
	
	private RuntimeWeaverParameters params;
	private File outputDir;
	private Weaver weaver; 
	
	/**
	 * Create a static weaver object.
	 * @param arg
	 */
	public StaticWeaver(String arg) {
		params = new RuntimeWeaverParameters(arg);
		outputDir = new File(params.getOutputDirname());
		WeaveConfig config = new WeaveConfig(params.getWeaveOption());
		weaver = new Weaver(new File(params.getOutputDirname()), config, params.getLoggingTargetOptions());
		
	}
	
	/**
	 * Execute the weaving process.
	 * @param filename
	 */
	public void weave(String filename) {
		File f = new File(filename);
		if (isClassFile(f)) {
			File wovenClassFile = new File(outputDir, f.getName()); 
			weaveFile(f, wovenClassFile);
		} else if (isJarFile(f) || isZipFile(f)) {
			File wovenFile = new File(outputDir, f.getName());
			weaveJarFileImpl(f, wovenFile);
		} else if (f.isDirectory()) {
			File wovenDir = new File(outputDir, f.getName());
			weaveDirectory(f, wovenDir);
		}
	}

	private byte[] executeWeave(String containerName, String classfileName, byte[] content) {
		// Check class name
		ClassReader reader = new ClassReader(content);
		if (params.isExcludedFromLogging(reader.getClassName())) {
			weaver.log("Excluded from weaving:" + classfileName);
			return content;
		}
		
		// Execute the weaving
		byte[] result = weaver.weave(null, classfileName, content, ClassLoader.getSystemClassLoader());
		if (result != null) {
			return result;
		} else {
			return content;
		}
	}
	
	/**
	 * Weave a file and generate a file.
	 * @param classFile is an input file.
	 * @param wovenClassFile is an output file.
	 */
	private void weaveFile(File classFile, File wovenClassFile) {
		wovenClassFile.getParentFile().mkdirs();
		try (FileOutputStream out = new FileOutputStream(wovenClassFile)) {
			byte[] content = Files.readAllBytes(classFile.toPath());
			byte[] result = executeWeave(null, classFile.getName(), content);
			out.write(result);
		} catch (IOException e) {
			weaver.log(e);
		}
	}

	/**
	 * Weave class files involved in a directory and its all sub-directories.
	 * @param dir specifies a directory. 
	 */
	public void weaveDirectory(File dir, File wovenDir) {
		LinkedList<File> dirs = new LinkedList<File>();
		dirs.add(dir);
		
		while (!dirs.isEmpty()) {
			File d = dirs.removeFirst();
			File[] files = d.listFiles();
			for (File f: files) {
				if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
					dirs.add(f);
				} else if (isJarFile(f)) {
					String relativePath = f.getAbsolutePath().substring(dir.getAbsolutePath().length() + File.separator.length());
					File outJar = new File(wovenDir, relativePath);
					weaveJarFileImpl(f, outJar);
				} else if (isClassFile(f)) {
					String relativePath = f.getAbsolutePath().substring(dir.getAbsolutePath().length() + File.separator.length());
					File wovenFile = new File(wovenDir, relativePath); 
					weaveFile(f, wovenFile);
				}
			}
		}
	}

	private void weaveJarFileImpl(File inputJarFile, File outputJarFile) {
		FileInputStream injar = null; 
		FileOutputStream outjar = null;
		ZipInputStream injarStream = null;
		JarOutputStream outjarStream = null;
		try {
			injar = new FileInputStream(inputJarFile);
			injarStream = new ZipInputStream(injar);
			outjar = new FileOutputStream(outputJarFile);
			outjarStream = new JarOutputStream(outjar);
			
			weaveClassesInJarFile(inputJarFile.getCanonicalPath(), injarStream, outjarStream);
			injarStream.close();
			outjarStream.close();
		} catch (IOException e) {
			weaver.log(e);
			try {
				if (injarStream != null) injarStream.close();
				else if (injar != null) injar.close();
			} catch (IOException e2) {
				weaver.log(e2);
			}
			try {
				if (outjarStream != null) outjarStream.close();
				else if (outjar != null) outjar.close();
			} catch (IOException e2) {
				weaver.log(e2);
			}
		}
		
	}

	/**
	 * Read fiels from inputJar, and then outputs woven classes to outputJar.
	 * This method is separated from the caller, because recursive calls.
	 * @param inputJar
	 * @param outputJar
	 */
	private void weaveClassesInJarFile(String inputjarName,  ZipInputStream inputJar, ZipOutputStream outputJar) throws IOException {
		for (ZipEntry entry = inputJar.getNextEntry(); entry != null; entry = inputJar.getNextEntry()) {  
			ZipEntry outEntry = new ZipEntry(entry.getName());
			outputJar.putNextEntry(outEntry);
			
			if (entry.getName().endsWith(".class")) { 
				// Weave a class file and write into the output jar
				byte[] target = streamToByteArray(inputJar, entry.getSize());
				byte[] result = executeWeave(inputjarName, entry.getName(), target);
				outputJar.write(result);
			} else if (entry.getName().endsWith(".jar") && weaveInternalJar) {
				// Load the internal jar and open it as a stream
				byte[] internalJar = streamToByteArray(inputJar, entry.getSize());
				ByteArrayInputStream b = new ByteArrayInputStream(internalJar);
				ZipInputStream internalJarStream = new ZipInputStream(b);
				
				// Create an output stream
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				JarOutputStream bufWriter = new JarOutputStream(buf);
				
				weaveClassesInJarFile(inputjarName + "/" + entry.getName(), internalJarStream, bufWriter);
				bufWriter.close();
				
				// Save the result to the parent JAR
				outputJar.write(buf.toByteArray());
			} else {
				// Just copy the content
				byte[] target = streamToByteArray(inputJar, entry.getSize());
				outputJar.write(target);
			}
			outputJar.closeEntry();
		}
	}
	
	/**
	 * Read the content from a stream.
	 * We assume that the target file is small enough. 
	 * This method does not close the input stream so that the caller side continues to use the stream. 
	 * @param in
	 * @param size
	 * @return
	 */
	private byte[] streamToByteArray(ZipInputStream in, long size) {
		try {
			if (size >= 0) {
				byte[] result = new byte[(int)size];
				DataInputStream din = new DataInputStream(in);
				din.readFully(result);
				return result;
			} else {
				// unknown file size
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buf = new byte[8192];
				int length;
				while ((length = in.read(buf)) > 0) {
					out.write(buf, 0, length);
				}
				return out.toByteArray();
			}
		} catch (IOException e) {
			weaver.log(e);
			return new byte[0];
		}
	}

	private static boolean isClassFile(File f) {
		return f.getAbsolutePath().endsWith(".class");
	}

	private static boolean isJarFile(File f) {
		return f.getAbsolutePath().endsWith(".jar");
	}

	private static boolean isZipFile(File f) {
		return f.getAbsolutePath().endsWith(".zip");
	}


}

