package it.voyage.ms.config.mongo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

/**
 *	Configuration for MongoDB.
 */
@Configuration
public class MongoDatabaseCFG {

	@Value("${data.mongodb.uri}")
	private String uri;
	

	@Value("${data.mongodb.schema-name}")
	private String schemaName;
	
    final List<Converter<?, ?>> conversions = new ArrayList<>();

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(){
    	  ConnectionString connectionString = new ConnectionString(uri);
          MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
              .applyConnectionString(connectionString)
              .build();
          return new SimpleMongoClientDatabaseFactory(MongoClients.create(mongoClientSettings), schemaName);
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(final MongoDatabaseFactory factory, final ApplicationContext appContext) {
        final MongoMappingContext mongoMappingContext = new MongoMappingContext();
        mongoMappingContext.setApplicationContext(appContext);
        MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return new MongoTemplate(factory, converter);
    }
  
    
    
 
}