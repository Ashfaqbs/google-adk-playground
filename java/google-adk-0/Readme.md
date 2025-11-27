# create a plain maven project

- add in pom.xml
```xml

   <dependency>
            <groupId>com.google.adk</groupId>
            <artifactId>google-adk</artifactId>
            <version>0.2.0</version>
        </dependency>
        <!-- Dev UI -->
        <dependency>
            <groupId>com.google.adk</groupId>
            <artifactId>google-adk-dev</artifactId>
            <version>0.2.0</version>
        </dependency>


```

- add env variables 
```
 GOOGLE_GENAI_USE_VERTEXAI=FALSE
 GOOGLE_API_KEY=PASTE_YOUR_ACTUAL_API_KEY_HERE
```

- Set Java 21 in cli if not set or not default kept 
```

$env:JAVA_HOME="C:\openjdk\jdk-21.0.8"
$env:Path="$env:JAVA_HOME\bin;$env:Path"


verify:

java -version
javac -version
mvn -v



mvn clean compile


```


- Run the project
```
CLI chat 

mvn compile exec:java "-Dexec.mainClass=com.example.MultiToolAgent"



UI

mvn exec:java "-Dexec.mainClass=com.google.adk.web.AdkWebServer" `
              "-Dexec.args=--adk.agents.source-dir=src/main/java" `                               
               "-Dexec.classpathScope=compile" 

```

