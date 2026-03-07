package com.learn.iterator;

import java.util.Iterator;

// write your code here
public class Node implements Iterable<Node> {

	int data;
	Node next;

	public Node(int data) {
		this.data = data;
	}

	public Node(int data, Node node) {
		this.data = data;
		this.next = node.next;
	}

	@Override
	public Iterator<Node> iterator() {
		return new NodeIterator(this);
	}

}

