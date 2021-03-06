package net.teamfruit.projectrtm.ngtlib.util;

import java.util.LinkedList;
import java.util.List;

public class Stack<E> {
	private List<E> list = new LinkedList<E>();
	private int maxSize;

	public Stack(int size) {
		this.maxSize = size;
	}

	public void push(E element) {
		while (this.list.size()>this.maxSize) {
			this.list.remove(this.list.size()-1);
		}
		this.list.add(0, element);
	}

	public E pop() {
		if (!this.list.isEmpty()) {
			E element = this.list.get(0);
			this.list.remove(0);
			return element;
		}
		return null;
	}
}