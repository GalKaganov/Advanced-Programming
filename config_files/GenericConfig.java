package test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
/**
 * GenericConfig reads a config file and creates agents accordingly,
 * wrapping each agent inside a ParallelAgent.
 */
public class GenericConfig implements Config {
    private String confFile = "";
    private List<ParallelAgent> agents = new ArrayList<>();

    public void setConfFile(String confFile) {
        if (!this.confFile.equals(confFile)) {
            this.confFile = confFile;
            close();  // close existing agents if config changed
        }
    }

    @Override
    public void create() {
        List<String> lines = readFile(confFile);
        if (lines.isEmpty()) {
            return; // Empty file, don't create any agents
        }

        if (lines.size() % 3 != 0) {
            System.err.println("Invalid config file format. Number of lines must be multiple of 3.");
            return;
        }

        // Clear any existing agents before creating new ones
        agents.clear();

        for (int i = 0; i < lines.size(); i += 3) {
            String className = lines.get(i).trim();
            String[] subs = lines.get(i + 1).trim().isEmpty() ? new String[0] : lines.get(i + 1).trim().split(",");
            String[] pubs = lines.get(i + 2).trim().isEmpty() ? new String[0] : lines.get(i + 2).trim().split(",");

            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> ctor = clazz.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) ctor.newInstance((Object) subs, (Object) pubs);
                agents.add(new ParallelAgent(agent, 10)); // use capacity 10 as example
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: " + className);
            } catch (NoSuchMethodException e) {
                System.err.println("Constructor not found in class: " + className);
            } catch (Exception e) {
                System.err.println("Failed to instantiate agent: " + className);
                e.printStackTrace();
            }
        }
    }

    private List<String> readFile(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Failed to read config file: " + filePath);
            e.printStackTrace();
        }
        return lines;
    }

    @Override
    public String getName() {
        return "GenericConfig";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void close() {
        // Create a copy of the list to avoid concurrent modification issues
        List<ParallelAgent> agentsCopy = new ArrayList<>(agents);
        for (ParallelAgent pa : agentsCopy) {
            pa.close();
        }
        agents.clear();
    }
}