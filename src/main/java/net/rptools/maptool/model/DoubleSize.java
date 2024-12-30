package net.rptools.maptool.model;

import java.awt.geom.Dimension2D;
import java.util.Objects;

public class DoubleSize extends Dimension2D {
    double width;
    double height;
    public DoubleSize(){
        this.width = 0;
        this.height = 0;
    }
    public DoubleSize(double width, double height){
        this.width = width;
        this.height = height;
    }
    public DoubleSize(Dimension2D d){
        this.width = d.getWidth();
        this.height = d.getHeight();
    }
    public DoubleSize(DoubleSize d){
        this.width = d.getWidth();
        this.height = d.getHeight();
    }
    public void setWidth(double width){
        this.width = width;
    }
    public void setHeight(double height){
        this.height = height;
    }
    @Override
    public double getWidth() {
        return this.width;
    }

    @Override
    public double getHeight() {
        return this.height;
    }

    @Override
    public void setSize(double width, double height) {
this.width = width;
this.height = height;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleSize size)) return false;
        return Double.compare(size.width, width) == 0 && Double.compare(size.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }
}
