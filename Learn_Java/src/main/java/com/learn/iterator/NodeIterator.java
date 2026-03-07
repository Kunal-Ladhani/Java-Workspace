package com.learn.iterator;

import java.util.Iterator;

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
