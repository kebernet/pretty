package com.reachcall.pretty.config;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;


/**
 *
 * @author kebernet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Path implements Cloneable, Serializable {
    private Configuration parent;
    private transient Pattern pattern;
    private String destination;
    private String source;

    /**
     * Set the value of destination
     *
     * @param newdestination new value of destination
     */
    public void setDestination(String newdestination) {
        this.destination = newdestination;
    }

    /**
     * Get the value of destination
     *
     * @return the value of destination
     */
    public String getDestination() {
        return this.destination;
    }

    /**
     * Set the value of parent
     *
     * @param newparent new value of parent
     */
    public void setParent(Configuration newparent) {
        this.parent = newparent;
    }

    /**
     * Get the value of parent
     *
     * @return the value of parent
     */
    @XmlTransient
    public Configuration getParent() {
        return this.parent;
    }

    /**
     * Set the value of source
     *
     * @param newsource new value of source
     */
    public void setSource(String newsource) {
        assert newsource != null : "Source path cannot be null";
        this.source = newsource;

        if (this.source != null) {
            this.pattern = Pattern.compile("^" + newsource);
        }
    }

    /**
     * Get the value of source
     *
     * @return the value of source
     */
    public String getSource() {
        return this.source;
    }

    public Match apply(String affinityId, String matchingPath,
        Configuration rootConfiguration) {
        Matcher m = pattern.matcher(matchingPath);

        if (!m.matches()) { //Interestingly, you have to call .matches() before group() works.

            return null;
        }

        int count = m.groupCount();

        if (count > 0) {
            return new Match(affinityId, this, this.destination + m.group(1),
                rootConfiguration);
        } else {
            return new Match(affinityId, this,
                matchingPath.startsWith("/")
                ? (destination + matchingPath.substring(1))
                : (destination + matchingPath), rootConfiguration);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Path other = (Path) obj;

        if ((this.destination == null) ? (other.destination != null)
                                           : (!this.destination.equals(
                    other.destination))) {
            return false;
        }

        if ((this.source == null) ? (other.source != null)
                                      : (!this.source.equals(other.source))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (41 * hash)
            + ((this.destination != null) ? this.destination.hashCode() : 0);
        hash = (41 * hash)
            + ((this.source != null) ? this.source.hashCode() : 0);

        return hash;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Path p = new Path();
        p.setDestination(this.destination);
        p.setSource(this.source);

        return p;
    }
}
