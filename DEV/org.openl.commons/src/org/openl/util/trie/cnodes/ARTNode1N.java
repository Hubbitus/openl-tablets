package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.nodes.NodeArrayIterator;

public final class ARTNode1N extends IARTNodeV.EmptyARTNodeV implements IARTNode {



	int startN;
	
	IARTNode[] nodes;

	private int countN;

	
	
	public ARTNode1N(int startN, int countN, IARTNode[] nodes)
	{
		this.nodes = nodes;
		this.startN = startN;
		this.countN = countN;
	}
	
	public ARTNode1N(int start, int count, int capacity)
	{
		this.startN = start;
		this.countN = count;
		this.nodes = new IARTNode[capacity];
	}
	
	
	
	@Override
	public IARTNode findNode(int index) {
		int idx = index - startN;
		if (idx < 0 || idx >= nodes.length)
			return null;
		return nodes[idx];
	}


	@Override
	public void setNode(int index, IARTNode node) {
		nodes[index - startN] = node;
		
	}


	



	@Override
	public int countN() {
		return countN ;
	}

	@Override
	public int minIndexN() {
		return startN;
	}

	@Override
	public int maxIndexN() {
		return startN + nodes.length -1;
	}


	@Override
	public IIntIterator indexIteratorN() {
		return NodeArrayIterator.iterator(startN, nodes);
	}


	@Override
	public IARTNode compact() { 
		return this;
	}

	

}
