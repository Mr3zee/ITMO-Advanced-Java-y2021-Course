javac ^
  -p "../../java-advanced-2021/artifacts" ^
  -cp "../../java-advanced-2021/modules/info.kgeorgiy.java.advanced.implementor" ^
  info/kgeorgiy/ja/sysoev/implementor/Implementor.java

jar cfm Implementor.jar META-INF/MANIFEST.MF info/kgeorgiy/ja/sysoev/implementor/*.class

del /S /Q *.class > NUL