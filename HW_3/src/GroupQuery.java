import java.util.Collection;
import java.util.List;

/**
 * Hard-version interface
 * for <a href="https://www.kgeorgiy.info/courses/java-advanced/homeworks.html#homework-student">Student</a> homework
 * of <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface GroupQuery extends StudentQuery {
    /**
     * Returns student groups, where both groups and students within a group are ordered by name.
     */
    List<Group> getGroupsByName(Collection<Student> students);

    /**
     * Returns student groups, where groups are ordered by name, and students within a group are ordered by id.
     */
    List<Group> getGroupsById(Collection<Student> students);

    /**
     * Returns group containing maximum number of students.
     * If there are more than one largest group, the one with greatest name is returned.
     */
    GroupName getLargestGroup(Collection<Student> students);

    /**
     * Returns group containing maximum number of students with distinct first names.
     * If there are more than one largest group, the one with smallest name is returned.
     */
    GroupName getLargestGroupFirstName(Collection<Student> students);
}
