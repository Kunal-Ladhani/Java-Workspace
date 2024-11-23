package com.learn.collection;

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

class NodeIterator implements Iterator<Node> {
	Node current;

	public NodeIterator() {}

	public NodeIterator(Node node) {
		this.current = node;
	}

	@Override
	public boolean hasNext() {
		return this.current != null;
	}

	@Override
	public Node next() {
		Node temp = this.current;
		this.current = this.current.next;
		return temp;
	}
}
