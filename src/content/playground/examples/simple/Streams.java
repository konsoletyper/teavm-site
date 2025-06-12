import java.util.Comparator;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        var people = List.of(
                new Person("Alice", 23),
                new Person("Bob", 17),
                new Person("Charlie", 30),
                new Person("Diana", 15)
        );

        var adultNames = people.stream()
                .filter(p -> p.age() >= 18)
                .sorted(Comparator.comparing(p -> p.name()))
                .map(Person::name)
                .toList();

        System.out.println("Adults: " + adultNames);
    }

    record Person(String name, int age) {}
}