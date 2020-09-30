package model;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicMap implements Serializable {
    private volatile Map<String, MapNode> nodes = new ConcurrentHashMap<>();
    private static final long serialVersionUID=1L;

    private void writeObject(ObjectOutputStream oos) throws Exception
    {
        oos.writeInt(nodes.values().size());
        for (MapNode value : nodes.values()) {
            oos.writeObject(value.getURL());
            oos.writeInt(value.getChildren().size());
            for (MapNode child : value.getChildren()) {
                oos.writeObject(child.getURL());
            }
        }
    }

    private void readObject(ObjectInputStream ois) throws Exception
    {
        nodes = new ConcurrentHashMap<>();
        int numNodes = ois.readInt();
        for (int i = 0; i < numNodes; i++) {
            String url = (String) ois.readObject();
            int numChildren = ois.readInt();
            List<MapNode> children = new ArrayList<>();
            for (int j = 0; j < numChildren; j++) {
                children.add(get((String) ois.readObject()));
            }
            create(url, children);
        }
    }

    private void create(String url, List<MapNode> children) {
        if(nodes.containsKey(url)) {
            MapNode node = nodes.get(url);
            node.initialize(children);
        }
        nodes.computeIfAbsent(url, s -> new MapNode(this, s, children));
    }

    public MapNode get(String url) {
        return nodes.computeIfAbsent(url, s-> new MapNode(this, s));
    }

    public void reset() {
        for (MapNode value : nodes.values()) {
            value.reset();
        }
    }
}
