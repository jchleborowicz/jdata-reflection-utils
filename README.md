# Info
This project contains a java code generator for populating object of specified class with random values.

Generator is usable only for experimenting or generating code for testing. 

You can quickly generate test objects by calling
 ```java
    pl.jdata.utils.reflection.ObjectConstructionGenerator.generate(Home.class);
```
This call writes output to stdout.
For sample Home class (included in tests) it generates following output:
 
```java
    final Home home = new Home();
    final Address address = new Address();
    address.setCity("LPMWX");
    address.setStreet("TaIKU");
    address.setBuildingNumber(73);
    address.setType(AddressType.HOME);
    address.setApartmentNumber(15);

    home.setAddress(address);

    home.setType("QevnM");

    final Set<String> colors = new HashSet<>();
    colors.add("tpckE");
    home.setColors(colors);
```
