package com.learn.nio;

import java.nio.charset.StandardCharsets;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class WriteFileNIO {
	public static void main(String[] args) throws IOException {
		
		// -----------------------WRITE TO A FILE----------------------------------------------
		
		Path p = Paths.get("newNioFile.txt");
		Files.write(p, "Written using NIO".getBytes(StandardCharsets.US_ASCII));
		// character set -> ASCII, UTF -> convert values to numbers and to bits -> UTF16 is most popular
		// can take Iterable as input -> list can also be passed
		
		// append will make sure that previous data is not cleared
		Files.write(p, Arrays.asList("one","two"),StandardOpenOption.APPEND);

		//--------------------------READING FROM A FILE---------------------------------------
		
		List<String> list = Files.readAllLines(p);
		for(String line : list) {
			System.out.println(line);
		}
		
		list.forEach(item -> System.out.println(item));
		
		
		// Find and replace using -> WITH HELP OF
		Stream<String> str = Files.lines(p);
		str
			.map(
					line -> {
						if(line.contains("using"))
							return line.replace("using", "with the help of");
						else
							return line;
					}
				).forEach(newLine -> System.out.println(newLine));
	}
}
