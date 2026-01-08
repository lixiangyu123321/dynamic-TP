import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

// 定义一个包含volatile int字段的类
class User {
// 必须是volatile int，不能是static，且访问权限至少能被updater访问
volatile int age;
private String name; // 普通字段，仅作示例

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', age=" + age + "}";
    }
}

public class AtomicIntegerFieldUpdaterDemo {
// 1. 创建Updater实例：指定目标类和目标字段名
private static final AtomicIntegerFieldUpdater<User> AGE_UPDATER =
AtomicIntegerFieldUpdater.newUpdater(User.class, "age");

    public static void main(String[] args) {
        User user = new User("张三", 20);

        // 2. 原子更新：CAS操作（如果当前age是20，就改成25）
        boolean updated = AGE_UPDATER.compareAndSet(user, 20, 25);
        System.out.println("CAS更新是否成功：" + updated); // 输出true
        System.out.println("更新后user：" + user); // 输出User{name='张三', age=25}

        // 3. 原子递增：age加1
        int newAge = AGE_UPDATER.incrementAndGet(user);
        System.out.println("递增后age：" + newAge); // 输出26

        // 4. 原子递减：age减2
        newAge = AGE_UPDATER.addAndGet(user, -2);
        System.out.println("递减后age：" + newAge); // 输出24
    }
}