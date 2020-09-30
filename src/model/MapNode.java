package model;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapNode {
    public static final String BASE_URL = "https://www.music-map.com/";
    private final String url;
    private final MusicMap musicMap;
    private int score = Integer.MAX_VALUE;
    private final List<MapNode> children;
    private final Set<MapNode> parents = new HashSet<>();
    private final AtomicBoolean weightsInitialized = new AtomicBoolean(false);
    private boolean childrenInitialized = false;
    private boolean childrenWeighted = false;

    MapNode(MusicMap musicMap, String url) {
        this(musicMap, url, new ArrayList<>());
    }

    MapNode(MusicMap musicMap, String url, List<MapNode> children) {
        this.children = children;
        this.musicMap = musicMap;
        this.url = url;
    }

    public List<MapNode> initialize() {
        if(childrenInitialized) {
            if(!childrenWeighted) {
                for(MapNode child: children) {
                    child.update(this);
                }
                childrenWeighted = true;
            }
            return children;
        }
        try {
            Document document = Jsoup.connect(BASE_URL + url).get();
            Element gnodMap = document.getElementById("gnodMap");
            for (Element child : gnodMap.children()) {
                String href = child.attr("href");
                if(href.startsWith("https://www.gnoosic.com"))
                    continue;
                MapNode node = musicMap.get(href);
                children.add(node);
                node.update(this);
            }
        } catch (IOException e) {
            if(!(e instanceof HttpStatusException) || ((HttpStatusException) e).getStatusCode() != 404) {
                e.printStackTrace();
                System.out.println(url);
            }
        }
        childrenInitialized = true;
        childrenWeighted = true;
        return Collections.unmodifiableList(children);
    }

    public void initialize(List<MapNode> children) {
        if(!childrenInitialized) {
            for(MapNode child: children) {
                this.children.add(child);
                child.update(this);
            }
            childrenInitialized = true;
        } else throw new RuntimeException("Double INIT!");
    }

    private synchronized void update(MapNode newParent) {
        parents.add(newParent);
        if(newParent.getScore() != Integer.MAX_VALUE && newParent.score + 1 < this.score) {
            this.score = newParent.score + 1;
        }
    }

    public synchronized void update(int score) {
        this.score = Math.min(this.score, score);
    }

    public int getScore() {
        return score;
    }

    public Set<MapNode> getParents() {
        return Collections.unmodifiableSet(parents);
    }

    public String getURL() {
        return url;
    }

    public boolean isChildrenInitialized() {
        return childrenInitialized;
    }

    public boolean isNotInitialized() {
        return !weightsInitialized.getAndSet(true);
    }

    void reset() {
        score = Integer.MAX_VALUE;
        weightsInitialized.set(false);
        childrenWeighted = false;
    }

    public List<MapNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public String toString() {
        return "MapNode{" +
                "url='" + url + '\'' +
                ", score=" + score +
                '}';
    }
}
