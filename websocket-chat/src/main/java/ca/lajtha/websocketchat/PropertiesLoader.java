package ca.lajtha.websocketchat;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Service for loading properties from configuration files.
 * Automatically discovers and loads all .properties files from the resources directory.
 */
public class PropertiesLoader {
    
    /**
     * Creates a PropertiesLoader that loads all .properties files from resources.
     */
    @Inject
    public PropertiesLoader() {
        // No configuration needed - will load all .properties files
    }
    
    /**
     * Loads properties from all .properties files found in the resources directory.
     * Files are loaded in the order they are discovered, with later files overriding
     * properties from earlier files if there are duplicate keys.
     * 
     * @return Properties object containing merged properties from all .properties files
     */
    public Properties loadProperties() {
        Properties props = new Properties();
        ClassLoader classLoader = PropertiesLoader.class.getClassLoader();
        
        // Scan for all .properties files
        scanResourcesForProperties(classLoader, props);
        
        // Fallback: try to load known common properties file if nothing was found
        if (props.isEmpty()) {
            loadPropertiesFile(classLoader, props, "server.properties");
        }
        
        return props;
    }
    
    /**
     * Scans the resources directory for all .properties files and loads them.
     * 
     * @param classLoader the class loader to use for resource access
     * @param props the Properties object to merge loaded properties into
     */
    private void scanResourcesForProperties(ClassLoader classLoader, Properties props) {
        try {
            // Try to get the classpath root
            URL rootUrl = classLoader.getResource("");
            if (rootUrl != null) {
                scanUrlForProperties(rootUrl, props, classLoader);
            }
            
            // Also try scanning from the package root
            Enumeration<URL> rootResources = classLoader.getResources("");
            while (rootResources.hasMoreElements()) {
                URL resourceUrl = rootResources.nextElement();
                scanUrlForProperties(resourceUrl, props, classLoader);
            }
            
        } catch (IOException e) {
            // If scanning fails, try alternative approach
            scanUsingFileSystem(classLoader, props);
        }
    }
    
    /**
     * Scans a URL for .properties files.
     */
    private void scanUrlForProperties(URL url, Properties props, ClassLoader classLoader) {
        try {
            String protocol = url.getProtocol();
            
            if ("file".equals(protocol)) {
                // File system - can directly list files
                Path path = Paths.get(url.toURI());
                if (Files.isDirectory(path)) {
                    Files.walk(path)
                        .filter(p -> p.toString().endsWith(".properties"))
                        .forEach(p -> loadPropertiesFromPath(p, props));
                }
            } else if ("jar".equals(protocol)) {
                // JAR file - need to use FileSystem
                String jarPath = url.getPath();
                int separatorIndex = jarPath.indexOf("!");
                if (separatorIndex > 0) {
                    String jarFile = jarPath.substring(5, separatorIndex); // Remove "file:" prefix
                    String pathInJar = jarPath.substring(separatorIndex + 1);
                    
                    try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:file:" + jarFile), Collections.emptyMap())) {
                        Path root = fs.getPath(pathInJar);
                        if (Files.exists(root)) {
                            Files.walk(root)
                                .filter(p -> p.toString().endsWith(".properties"))
                                .forEach(p -> loadPropertiesFromPath(p, props));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If this approach fails, continue with alternative methods
        }
    }
    
    /**
     * Alternative scanning method that tries to find .properties files by name.
     * This is a fallback when directory scanning isn't possible (e.g., in some JAR scenarios).
     */
    private void scanUsingFileSystem(ClassLoader classLoader, Properties props) {
        // Try to find .properties files by attempting to load them directly
        // This works when we know the file names or can guess common patterns
        // For a more complete solution, you might want to use a library like Spring's ResourcePatternResolver
        // or maintain a list of known properties files
        
        // Try common property file names
        String[] commonNames = {"server.properties", "application.properties", "config.properties"};
        for (String fileName : commonNames) {
            loadPropertiesFile(classLoader, props, fileName);
        }
    }
    
    /**
     * Loads properties from a file path.
     */
    private void loadPropertiesFromPath(Path path, Properties props) {
        try (InputStream input = Files.newInputStream(path)) {
            Properties fileProps = new Properties();
            fileProps.load(input);
            props.putAll(fileProps);
            System.out.println("Loaded properties from: " + path);
        } catch (IOException e) {
            System.err.println("Warning: Error loading properties from " + path + ": " + e.getMessage());
        }
    }
    
    /**
     * Loads a specific properties file by name.
     * 
     * @param classLoader the class loader to use
     * @param props the Properties object to merge into
     * @param fileName the name of the properties file
     */
    private void loadPropertiesFile(ClassLoader classLoader, Properties props, String fileName) {
        try (InputStream input = classLoader.getResourceAsStream(fileName)) {
            if (input != null) {
                Properties fileProps = new Properties();
                fileProps.load(input);
                props.putAll(fileProps);
                System.out.println("Loaded properties from: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Warning: Error loading " + fileName + ": " + e.getMessage());
        }
    }
    
    /**
     * Gets a property value with a default fallback.
     * 
     * @param props the Properties object to read from
     * @param key the property key
     * @param defaultValue the default value if key is not found
     * @return the property value or default value
     */
    public String getProperty(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
    
    /**
     * Gets an integer property value with a default fallback.
     * 
     * @param props the Properties object to read from
     * @param key the property key
     * @param defaultValue the default value if key is not found or cannot be parsed
     * @return the integer property value or default value
     */
    public int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Gets a boolean property value with a default fallback.
     * 
     * @param props the Properties object to read from
     * @param key the property key
     * @param defaultValue the default value if key is not found
     * @return the boolean property value or default value
     */
    public boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}

