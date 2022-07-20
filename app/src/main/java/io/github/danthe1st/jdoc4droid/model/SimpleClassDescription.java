package io.github.danthe1st.jdoc4droid.model;

import java.io.Serializable;
import java.util.Objects;

public class SimpleClassDescription implements Serializable {
	private static final long serialVersionUID = 163640046407057923L;

	private String name;
	private String description;
	private String classType;
	private String packageName;
	private String path;

	//region boilerplate
	public SimpleClassDescription(String name, String description, String classType, String packageName, String path) {
		this.name = name;
		this.description = description;
		this.classType = classType;
		this.packageName = packageName;
		this.path = path;
	}

	public String getFullName() {
		return getPackageName() + "." + getName();
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public String getClassType() {
		return this.classType;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public String getPath() {
		return this.path;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		SimpleClassDescription that = (SimpleClassDescription) o;
		return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(classType, that.classType) && Objects.equals(packageName, that.packageName) && Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, classType, packageName, path);
	}

	@Override
	public String toString() {
		return "SimpleClassDescription{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", classType='" + classType + '\'' +
				", packageName='" + packageName + '\'' +
				", path='" + path + '\'' +
				'}';
	}
	//endregion
}
