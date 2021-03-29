SET external=../../java-advanced-2021/modules/info.kgeorgiy.java.advanced.
SET extPackage=%external%implementor/info/kgeorgiy/java/advanced/implementor/
SET lib=../../java-advanced-2021/lib/

javadoc ^
  -private -version -author ^
  -p %lib%junit-4.11.jar;%external%implementor;%external%base; ^
  -cp %external%base;%%external%implementor;../../java-advanced-2021/lib/junit-4.11.jar; ^
  -d javadoc/ ^
  -docencoding utf-8 ^
  info.kgeorgiy.ja.sysoev.implementor %extPackage%Impler.java %extPackage%JarImpler.java %extPackage%ImplerException.java
