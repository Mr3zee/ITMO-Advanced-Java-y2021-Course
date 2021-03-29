module info.kgeorgiy.ja.sysoev.implementor {
    requires transitive info.kgeorgiy.java.advanced.implementor;
    requires java.management;
    requires java.compiler;

    exports info.kgeorgiy.ja.sysoev.implementor;

    opens info.kgeorgiy.ja.sysoev.implementor to junit;
}