package com.learn.nio;

import java.io.IOException;
import java.nio.file.*;

public class LearnNIO {
	public static void main(String[] args) throws IOException {
		
		// ----------------CREATE A NEW FILE---------------------
		
		Path filePath = Paths.get("newNioFile.txt");
		// Paths -> final class
		// gets -> static method
		
		// check if file exists
		if(Files.exists(filePath) == true) {
			System.out.println("File already Exists.");
		} else {
			Path path = Files.createFile(filePath);
			System.out.println("Created a file at "+path);
		}
	}
}
