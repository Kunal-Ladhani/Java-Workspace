package com.design.hashmap.ideal;

public class MyHashMap<K, V> {

	private static final int DEFAULT_CAPACITY = 16;
	private static final float LOAD_FACTOR = 0.75f;
	private static final int MAX_CAPACITY = 1 << 30;

	private Node<K, V>[] table;
	private int size;

	public MyHashMap() {
		table = new Node[DEFAULT_CAPACITY];
		size = 0;
	}

	/* ================== PUBLIC APIs ================== */

	public V get(K key) {
		Node<K, V> node = getNode(key);
		return node == null ? null : node.value;
	}

	public void put(K key, V value) {
		if (key == null) {
			throw new IllegalArgumentException("Null keys not supported");
		}

		int index = indexFor(key, table.length);
		Node<K, V> head = table[index];

		// overwrite if key exists
		for (Node<K, V> curr = head; curr != null; curr = curr.next) {
			if (curr.key.equals(key)) {
				curr.value = value;
				return;
			}
		}

		// insert at head
		table[index] = new Node<>(key, value, head);
		size++;

		if (size > table.length * LOAD_FACTOR) {
			resize();
		}
	}

	public void remove(K key) {
		int index = indexFor(key, table.length);
		Node<K, V> curr = table[index];
		Node<K, V> prev = null;

		while (curr != null) {
			if (curr.key.equals(key)) {
				if (prev == null) {
					table[index] = curr.next;
				} else {
					prev.next = curr.next;
				}
				size--;
				return;
			}
			prev = curr;
			curr = curr.next;
		}
	}

	public boolean containsKey(K key) {
		return getNode(key) != null;
	}

	public int size() {
		return size;
	}

	/* ================== INTERNAL HELPERS ================== */

	private Node<K, V> getNode(K key) {
		int index = indexFor(key, table.length);
		Node<K, V> curr = table[index];

		while (curr != null) {
			if (curr.key.equals(key)) {
				return curr;
			}
			curr = curr.next;
		}
		return null;
	}

	private int indexFor(K key, int length) {
		int hash = key.hashCode();
		return (hash & 0x7fffffff) & (length - 1);
	}

	private void resize() {
		int oldCap = table.length;
		if (oldCap >= MAX_CAPACITY) return;

		int newCap = oldCap << 1;
		Node<K, V>[] newTable = new Node[newCap];

		for (Node<K, V> head : table) {
			while (head != null) {
				Node<K, V> next = head.next;
				int index = indexFor(head.key, newCap);

				head.next = newTable[index];
				newTable[index] = head;

				head = next;
			}
		}
		table = newTable;
	}
}