package com.metaweb.gridworks.protograph;

public interface NodeWithLinks {
    public void addLink(Link link);
    
    public int getLinkCount();
    
    public Link getLink(int index);
}
