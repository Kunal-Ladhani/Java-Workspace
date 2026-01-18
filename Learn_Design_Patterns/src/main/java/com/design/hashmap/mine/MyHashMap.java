package com.design.hashmap.mine;

public class MyHashMap<K, V> {

	public static final float LOAD_FACTOR = 0.75f;
	public static final int INITIAL_SIZE = 4;
	public static final int MAX_CAPACITY = 1 << 30;

	private int totalNodes;
	private Node<K, V>[] bucketsArray;

	public MyHashMap() {
		this.totalNodes = 0;
		bucketsArray = new Node[INITIAL_SIZE];
		for (int i = 0; i < INITIAL_SIZE; i++) {
			// make dummy nodes
			bucketsArray[i] = new Node<>(null, null);    // head node
			bucketsArray[i].next = new Node<>(null, null);    // tail node
			bucketsArray[i].next.prev = bucketsArray[i];    // next node refs to curr node
		}
	}

	public int size() {
		return bucketsArray.length;
	}

	public V get(K key) {
		Node<K, V> nodeToGet = findNode(key);
		if (nodeToGet != null) {
			return nodeToGet.val;
		}
		return null;
	}

	public void put(K key, V value) {
		Node<K, V> node = findNode(key);
		if (node != null) {
			node.key = key;
			node.val = value;
			return;
		}

		int bucket = findBucket(key, bucketsArray.length);
		Node<K, V> head = bucketsArray[bucket];

		Node<K, V> oldFirst = head.next;
		Node<K, V> newNode = new Node<>(key, value);
		head.next = newNode;
		newNode.prev = head;
		newNode.next = oldFirst;
		oldFirst.prev = newNode;
		totalNodes++;

		if (totalNodes > LOAD_FACTOR * bucketsArray.length) {
			reHash(bucketsArray.length * 2);
		}
	}

	public void remove(K key) {
		Node<K, V> nodeToRemove = findNode(key);
		if (nodeToRemove == null) {
			return;
		}

		Node<K, V> prevPtr = nodeToRemove.prev;
		Node<K, V> nextPtr = nodeToRemove.next;

		prevPtr.next = nextPtr;
		nextPtr.prev = prevPtr;

		// perform cleanup of removed node
		nodeToRemove.prev = null;
		nodeToRemove.next = null;

		totalNodes--;
	}

	// helper functions
	private Node<K, V> findNode(K key) {
		int bucket = findBucket(key, bucketsArray.length);
		Node<K, V> curr = bucketsArray[bucket];

		while (curr != null) {
			if (curr.key != null && curr.key.equals(key)) {
				return curr;
			}
			curr = curr.next;
		}
		return null;
	}

	private void reHash(int newSize) {
		if (newSize > MAX_CAPACITY) {
			throw new UnsupportedOperationException("Hashmap is exceeding max capacity");
		}

		Node<K, V>[] newBucketArray = new Node[newSize];
		for (int i = 0; i < newSize; i++) {
			newBucketArray[i] = new Node<>(null, null); // dummy head node
			newBucketArray[i].next = new Node<>(null, null); // dummy tail node
			newBucketArray[i].next.prev = newBucketArray[i];
		}

		for (Node<K, V> curr : bucketsArray) {
			if (curr.key == null) {
				curr = curr.next;
				continue;
			}

			int newBucket = findBucket(curr.key, newSize);
			Node<K, V> newHead = newBucketArray[newBucket];
			Node<K, V> oldFirst = newHead.next;

			Node<K, V> nextNode = curr.next;    // saving ptr to next node in old LL

			newHead.next = curr;
			curr.prev = newHead;
			curr.next = oldFirst;
			oldFirst.prev = curr;

			curr = nextNode;
		}
		bucketsArray = newBucketArray;
	}

	// works only if len = pow of 2
	// keeps only lower bits [log2(size) num of bits kept rest 0] as is!
	// result always between -> 0 to size-1 -> same thing we wanted with % but it causes problem with -ve hashcodes!
	private int findBucket(K key, int size) {
		return key.hashCode() & (size - 1);
	}

}