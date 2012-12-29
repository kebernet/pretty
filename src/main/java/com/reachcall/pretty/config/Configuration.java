package com.reachcall.pretty.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;


/**
 *
 * @author kebernet
 */
@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Configuration {
    private Configuration parent;
    private Integer maxRequestsByHost;
    private List<Configuration> children = new LinkedList<Configuration>();
    private List<Destination> destinations;
    private List<Path> paths = new LinkedList<Path>();
    private Pattern pattern;
    private String hostPattern;
    private String rewriteFileName;
    private boolean inactive;

    /**
     * Set the value of children
     *
     * @param newchildren new value of children
     */
    public void setChildren(List<Configuration> newchildren) {
        this.children = newchildren;
    }

    /**
     * Get the value of children
     *
     * @return the value of children
     */
    @XmlElementWrapper(name = "children")
    @XmlElement(name = "configuration")
    public List<Configuration> getChildren() {
        for (Configuration c : this.children) {
            c.setParent(this);
        }

        return this.children;
    }

    /**
     * Set the value of destination
     *
     * @param newdestination new value of destination
     */
    public void setDestinations(List<Destination> newdestination) {
        this.destinations = newdestination;
    }

    /**
     * Get the value of destination
     *
     * @return the value of destination
     */
    @XmlElementWrapper(name = "destinations")
    @XmlElement(name = "destination")
    public List<Destination> getDestinations() {
        return this.destinations;
    }

    /**
     * Set the value of hostPattern
     *
     * @param newhostPattern new value of hostPattern
     */
    public void setHostPattern(String newhostPattern) {
        this.hostPattern = newhostPattern;
        this.pattern = Pattern.compile(hostPattern);
    }

    /**
     * Get the value of hostPattern
     *
     * @return the value of hostPattern
     */
    @XmlElement(name = "host-pattern")
    public String getHostPattern() {
        return this.hostPattern;
    }

    /**
     * Set the value of inactive
     *
     * @param newinactive new value of inactive
     */
    public void setInactive(boolean newinactive) {
        this.inactive = newinactive;

        for (Configuration c : this.children) {
            c.setInactive(true);
        }
    }

    /**
     * Get the value of inactive
     *
     * @return the value of inactive
     */
    @XmlTransient
    public boolean isInactive() {
        return this.inactive;
    }

    /**
     * Set the value of maxRequestsByHost
     *
     * @param newmaxRequestsByHost new value of maxRequestsByHost
     */
    public void setMaxRequestsByHost(Integer newmaxRequestsByHost) {
        this.maxRequestsByHost = newmaxRequestsByHost;
    }

    /**
     * Get the value of maxRequestsByHost
     *
     * @return the value of maxRequestsByHost
     */
    @XmlElement(name = "max-requests-by-host")
    public Integer getMaxRequestsByHost() {
        return ((this.maxRequestsByHost == null) && (parent != null))
        ? parent.getMaxRequestsByHost() : this.maxRequestsByHost;
    }

    /**
     * Set the value of parent
     *
     * @param newparent new value of parent
     */
    public synchronized void setParent(Configuration newparent) {
        if (this.parent != newparent) { // DON'T DO THIS TWICE
            this.parent = newparent;

            for (Path p : parent.getPaths()) {
                try {
                    // COPY PARENT PATHS INTO THE CHILD
                    Path copy = (Path) p.clone();
                    this.paths.add(copy);
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(Configuration.class.getName())
                          .log(Level.SEVERE, null, ex);
                }
            }
        }
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
     * Set the value of paths
     *
     * @param newpaths new value of paths
     */
    public void setPaths(List<Path> newpaths) {
        this.paths = newpaths;
    }

    /**
     * Get the value of paths
     *
     * @return the value of paths
     */
    @XmlElementWrapper(name = "paths")
    @XmlElement(name = "path")
    public List<Path> getPaths() {
        if (this.paths == null) {
            return Collections.EMPTY_LIST;
        }

        for (Path p : this.paths) {
            p.setParent(this);
        }

        return this.paths;
    }

    /**
     * Set the value of rewriteFileName
     *
     * @param newrewriteFileName new value of rewriteFileName
     */
    public void setRewriteFileName(String newrewriteFileName) {
        this.rewriteFileName = newrewriteFileName;
    }

    /**
     * Get the value of rewriteFileName
     *
     * @return the value of rewriteFileName
     */
    @XmlElement(name = "rewrite-file-name")
    public String getRewriteFileName() {
        return ((this.rewriteFileName == null) && (parent != null))
        ? parent.getRewriteFileName() : this.rewriteFileName;
    }

    public Match apply(String affinityId, String host, String path,
        Configuration rootConfiguration) {
        if (pattern.matcher(host)
                       .matches()) {
            for (Configuration config : this.getChildren()) {
                Match child = config.apply(affinityId, host, path,
                        rootConfiguration);

                if (child != null) {
                    return child;
                }
            }

            for (Path p : this.getPaths()) {
                Match applied = p.apply(affinityId, path, rootConfiguration);

                if (applied != null) {
                    return applied;
                }
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Configuration other = (Configuration) obj;

        if ((this.destinations != other.destinations)
                && ((this.destinations == null)
                || !this.destinations.equals(other.destinations))) {
            return false;
        }

        if ((this.maxRequestsByHost != other.maxRequestsByHost)
                && ((this.maxRequestsByHost == null)
                || !this.maxRequestsByHost.equals(other.maxRequestsByHost))) {
            return false;
        }

        if ((this.children != other.children)
                && ((this.children == null)
                || !this.children.equals(other.children))) {
            return false;
        }

        if ((this.paths != other.paths)
                && ((this.paths == null) || !this.paths.equals(other.paths))) {
            System.out.println("Paths mismatch");

            return false;
        }

        if ((this.hostPattern == null) ? (other.hostPattern != null)
                                           : (!this.hostPattern.equals(
                    other.hostPattern))) {
            return false;
        }

        if ((this.rewriteFileName == null) ? (other.rewriteFileName != null)
                                               : (!this.rewriteFileName.equals(
                    other.rewriteFileName))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = (53 * hash)
            + ((this.destinations != null) ? this.destinations.hashCode() : 0);
        hash = (53 * hash)
            + ((this.maxRequestsByHost != null)
            ? this.maxRequestsByHost.hashCode() : 0);
        hash = (53 * hash)
            + ((this.children != null) ? this.children.hashCode() : 0);
        hash = (53 * hash) + ((this.paths != null) ? this.paths.hashCode() : 0);
        hash = (53 * hash)
            + ((this.hostPattern != null) ? this.hostPattern.hashCode() : 0);
        hash = (53 * hash)
            + ((this.rewriteFileName != null) ? this.rewriteFileName.hashCode()
                                              : 0);

        return hash;
    }
}
