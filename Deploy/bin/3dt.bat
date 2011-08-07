echo off

set JAVA=$JAVA_HOME\bin\java
set BASE=$INSTALL_PATH

set GAVROG=%BASE%\Systre.jar;%BASE%\3dt-Main.jar
set JREAL=%BASE%\jReality\jReality.jar
set JREAL_PLUS=%BASE%\jReality\bsh.jar;%BASE%\jReality\jtem-beans.jar;%BASE%\jReality\jTerm.jar
set XSTREAM=%BASE%\XStream\xpp3.jar;%BASE%\XStream\xstream.jar
set JOGL=%BASE%\jogl\jogl.jar;%BASE%\jogl\gluegen-rt.jar
set SUNFLOW=%BASE%\sunflow\sunflow.jar;%BASE%\sunflow\janino.jar

set CLASSPATH=%GAVROG%;%JREAL%;%JREAL_PLUS%;%XSTREAM%;%JOGL%;%SUNFLOW%

"%JAVA%" -Xmx512m -D3dt.home="%BASE%" -Djava.library.path="%BASE%\jogl" -cp "%CLASSPATH%" org.gavrog.apps._3dt.Main %*

exit
