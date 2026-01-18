package com.design.hashmap.mine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node<K, V> {
	public Node<K,V> next;
	public Node<K,V> prev;
	public K key;
	public V val;

	public Node(K key, V val) {
		this.key = key;
		this.val = val;
		this.next = null;
		this.prev = null;
	}

}
