import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements GroupQuery {

    @FunctionalInterface
    public interface StudentGetter<T> extends Function<Student, T> {
        T apply(Student student);
    }

    private <T> Stream<T> getAttributeStream(List<Student> students, StudentGetter<T> getter) {
        return students.stream().map(getter);
    }

    private <T> List<T> getAttribute(List<Student> students, StudentGetter<T> getter) {
        return getAttributeStream(students, getter).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getAttribute(students, Student::getFirstName);
    }

    private Stream<String> getFirstNamesStream(List<Student> students) {
        return getAttributeStream(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getAttribute(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getAttribute(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getAttribute(students, s -> String.format("%s %s", s.getFirstName(), s.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFirstNamesStream(students).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse(null);
    }

    private Stream<Student> sortStream(Stream<Student> students, Comparator<Student> comparator) {
        return students.sorted(comparator);
    }

    private Stream<Student> sortStream(Collection<Student> students, Comparator<Student> comparator) {
        return sortStream(students.stream(), comparator);
    }

    private List<Student> sort(Collection<Student> students, Comparator<Student> comparator) {
        return sortStream(students, comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sort(students, Student::compareTo);
    }

    private final Comparator<Student> nameOrder = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparing(Student::getId);

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sort(students, nameOrder);
    }

    private <T> Stream<Student> filterStream(Collection<Student> students, StudentGetter<T> getter, T atr) {
        return students.stream().filter(s -> getter.apply(s).equals(atr));
    }

    private <T> Stream<Student> filterSort(
            Collection<Student> students,
            StudentGetter<T> getter,
            T atr,
            Comparator<Student> comparator
    ) {
        return sortStream(filterStream(students, getter, atr), comparator);
    }

    private <T> Stream<Student> filterSortByNameStream(Collection<Student> students, StudentGetter<T> getter, T atr) {
        return filterSort(students, getter, atr, nameOrder);
    }

    private <T> List<Student> filterSortByName(Collection<Student> students, StudentGetter<T> getter, T atr) {
        return filterSortByNameStream(students, getter, atr).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterSortByName(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterSortByName(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterSortByName(students, Student::getGroup, group);
    }

    private Stream<Student> findStudentsByGroupStream(Collection<Student> students, GroupName group) {
        return filterStream(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroupStream(students, group)
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        (s1, s2) -> (s1.compareTo(s2) > 0 ? s2 : s1)
//                        TreeMap::new
                ));
    }

    private <T> Stream<Map.Entry<GroupName, T>> mapGroupsToStudents(
            Collection<Student> students,
            Comparator<GroupName> groupNameComparator,
            Collector<? super Student, ?, T> studentCollector
    ) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        Student::getGroup,
                        () -> new TreeMap<>(groupNameComparator),
                        studentCollector
                ))
                .entrySet().stream();
    }

    private <T> Stream<Map.Entry<GroupName, T>> mapGroupsToStudents(
            Collection<Student> students,
            Collector<? super Student, ?, T> studentCollector
    ) {
        return mapGroupsToStudents(students, GroupName::compareTo, studentCollector);
    }

    private Stream<Map.Entry<GroupName, List<Student>>> mapGroupsToStudentList(Collection<Student> students) {
        return mapGroupsToStudents(students, Collectors.toList());
    }

    private List<Group> collectGroups(
            Collection<Student> students,
            Function<Collection<Student>, List<Student>> sortStudents
    ) {
        return mapGroupsToStudentList(students)
                .map(e -> new Group(e.getKey(), sortStudents.apply(e.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return collectGroups(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return collectGroups(students, this::sortStudentsById);
    }

    public <T extends Comparable<? super T>> GroupName getLargestGroup(
            Collection<Student> students,
            Collector<? super Student, ?, T> collector,
            Comparator<? super String> comparator
    ) {
        return mapGroupsToStudents(students, collector)
                .sorted(Comparator.comparing(
                        (Map.Entry<GroupName, T> e) -> e.getKey().toString(),
                        comparator
                )).max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return this.<Long>getLargestGroup(
                students,
                Collectors.counting(),
                Comparator.comparing((String s) -> s).reversed()
        );
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return this.<Integer>getLargestGroup(
                students,
                Collectors.collectingAndThen(
                        Collectors.mapping(Student::getFirstName, Collectors.toSet()),
                        Set::size
                ),
                String::compareTo
        );
    }
}
