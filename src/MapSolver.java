import model.MapNode;
import model.MusicMap;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MapSolver {
    public static final File log = new File("output.log");
    public static final File file = new File("map.dat");
    public static final boolean PRINT_SOLUTIONS = true;
    public static final int TOLERANCE = 0;
    public static void main(String[] args) {
        PrintStream out = null;
        try {
            if(!log.delete())
                System.out.println("FAILED TO DELETE");
            out = new PrintStream(new FileOutputStream(log));
            System.setOut(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        MusicMap musicMap;
        try {
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            musicMap = (MusicMap) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            musicMap = new MusicMap();
        }
        new MapSolver(musicMap, "the+beatles", "miles+davis");
        new MapSolver(musicMap, "louis+armstrong", "nickelback");
        new MapSolver(musicMap, "mozart", "jacob+collier");
        new MapSolver(musicMap, "nicki+minaj", "chick+corea");
        new MapSolver(musicMap, "billie+eilish", "count+basie+orchestra");
        try {
            boolean delete = file.delete();
            if(!delete)
                System.out.println("Failed to Delete!");
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            oos.writeObject(musicMap);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(out != null)
            out.close();
        System.exit(0);
    }
    private final BlockingQueue<MapNode> horizon = new ArrayBlockingQueue<>(4096);
    private final Set<MapNode> visited = new HashSet<>();
    public MapSolver(MusicMap musicMap, String startString, String endString) {
        System.out.println(" --- " + startString + " -> " + endString + " --- ");
        musicMap.reset();
        MapNode start = musicMap.get(startString);
        MapNode goal = musicMap.get(endString);
        MapNode current = start;
        current.update(0);
        ExecutorService service = Executors.newFixedThreadPool(20);
        while(goal.getScore() == Integer.MAX_VALUE || goal.getScore() + TOLERANCE > current.getScore()) {
            List<Callable<Boolean>> runnables = new ArrayList<>();
            if(current.isNotInitialized()) {
                MapNode finalCurrent = current;
                Callable<Boolean> run = () -> {
                    if (goal.getScore() < Integer.MAX_VALUE && goal.getScore() + TOLERANCE <= finalCurrent.getScore())
                        return true;
                    List<MapNode> initialize = finalCurrent.initialize();
                    if(horizon.remainingCapacity() < initialize.size())
                        return false;
                    for (MapNode node : initialize) {
                        if (!visited.contains(node)) {
                            visited.add(node);
                            horizon.add(node);
                        }
                    }
                    return true;
                };
                if(current.isChildrenInitialized()) {
                    try {
                        if(!run.call())
                            runnables.add(run);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    runnables.add(run);
                }
            }
            try {
                for (Future<Boolean> booleanFuture : service.invokeAll(runnables)) {
                    if(!booleanFuture.get())
                        throw new RuntimeException("We had a failure!");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
//            if(visited.size() % 128 == 0)
//                System.out.println(visited.size());
            try{
                current = horizon.take();
            }catch(NoSuchElementException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        TreeMap<Integer, List<List<MapNode>>> solutionCount = new TreeMap<>();
        makeAllPaths(new ArrayList<>(), goal, start, TOLERANCE, solutionCount);
        printAllPaths(solutionCount);

        service.shutdownNow();
    }

    void makeAllPaths(List<MapNode> path, MapNode current, MapNode goal, int tolerance, Map<Integer, List<List<MapNode>>> solutionCount) {
        if(current == goal) {
            path.add(current);
            solutionCount.computeIfAbsent(path.size(), s->new ArrayList<>()).add(path);
            return;
        }
        path.add(current);
        Optional<MapNode> mapNode = current.getParents().stream().min(Comparator.comparingInt(MapNode::getScore));
        int minScore = mapNode.map(MapNode::getScore).orElse(Integer.MAX_VALUE);
        if(minScore >= current.getScore() + tolerance)
            return;
        for (MapNode parent : current.getParents()) {
            if(parent.getScore() > minScore + tolerance)
                continue;
            makeAllPaths(new ArrayList<>(path), parent, goal,
                    tolerance - (parent.getScore() - minScore), solutionCount);
        }
    }

    private void printAllPaths(TreeMap<Integer, List<List<MapNode>>> solutionCount) {
        Integer minimum = solutionCount.firstKey();
        for (Map.Entry<Integer, List<List<MapNode>>> entry : solutionCount.entrySet()) {
            if(PRINT_SOLUTIONS) {
                if(entry.getKey().equals(minimum))
                    System.out.println("Solutions:");
                else
                    System.out.println("Approximations (Off by "+(entry.getKey() - minimum)+"):");
                for (List<MapNode> path : entry.getValue()) {
                    Collections.reverse(path);
                    System.out.print("\t"+path.stream().map(MapNode::getURL).collect(Collectors.joining(", ")));
                    System.out.println();
                }

            }else{
                System.out.println("Length "+entry.getKey()+": "+entry.getValue().size()+" Possibilities");
            }
        }
    }
}
