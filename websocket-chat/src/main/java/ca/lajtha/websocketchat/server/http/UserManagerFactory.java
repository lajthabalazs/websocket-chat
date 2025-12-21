package ca.lajtha.websocketchat.server.http;

import ca.lajtha.websocketchat.PropertiesLoader;
import ca.lajtha.websocketchat.user.InMemoryUserDatabase;
import ca.lajtha.websocketchat.user.UserDatabase;
import ca.lajtha.websocketchat.user.UserManager;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * Factory for creating UserManager and its dependencies as Micronaut beans.
 */
@Factory
public class UserManagerFactory {
    
    @Bean
    @Singleton
    public PropertiesLoader propertiesLoader() {
        return new PropertiesLoader();
    }
    
    @Bean
    @Singleton
    public UserDatabase userDatabase() {
        return new InMemoryUserDatabase();
    }
    
    @Bean
    @Singleton
    public UserManager userManager(PropertiesLoader propertiesLoader, UserDatabase userDatabase) {
        return new UserManager(userDatabase, propertiesLoader);
    }
}

