# KIRHelperKit and Case Study
This project includes the KIRHelperKit (see [KIRHelperKit](KIRHelperKit/README.md)) alongside with two small compiler plugins used for a case study.

## Overview
### stringbuilder_plugin
This plugin saves the name of each function for each invocation to a stringbuilder and prints the resulting string at the end of the main function.
This plugin is used to explain the general workings of the KIRHelperKit.

### atomic_int_plugin
This plugin increases an atomic integer each time a function is invoked and prints the resulting value at the end of the main function.
This plugin should be implemented by the colleges in my case study.

---

## 📦 Setup Instructions

### 1. Download the Project
Download the repository as a ZIP:  
[https://github.com/LorenzBader/KIRHelperKit_and_case_study](https://github.com/LorenzBader/KIRHelperKit_and_case_study)  

Unzip and open in **IntelliJ IDEA**.

---

### 2. Configure JDK 21
This project requires **JDK 21**.

If you don’t have JDK 21 installed:
1. Download from a reliable source:  
   [Adoptium Temurin JDK 21](https://adoptium.net/en-GB/temurin/releases/?version=21)
2. Install and set the JDK path in `gradle.properties`:
   ```properties
   org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-21
   ```

---

### 3. Build and Publish `KIRHelperKit`
Before working with the example projects, **link** `KIRHelperKit` in IntelliJ IDEA and build it:

In the terminal (inside the `KIRHelperKit` directory):
```bash
./gradlew build
./gradlew publishToMavenLocal
```

This will publish `KIRHelperKit` to your local Maven repository, allowing the example projects to detect it.

---

### 4. Open Example Projects
After `KIRHelperKit` is published locally:

1. Open and **link** the `stringbuilder` project in IntelliJ IDEA (Gradle project).
2. Open and **link** the `atomicint` project in IntelliJ IDEA (Gradle project).

Both projects should now correctly resolve the `KIRHelperKit` dependency.

---

## 💡 Notes
- Always build and publish `KIRHelperKit` first if you make changes to it before running the examples.
- Ensure IntelliJ is using the correct Gradle JDK (JDK 21).
