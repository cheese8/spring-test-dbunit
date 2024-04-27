package com.github.springtestdbunit.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;

import com.github.springtestdbunit.DbUnitTestContext;

public class Annotations<T extends Annotation> implements Iterable<T> {

	private final List<T> classAnnotations;

	private final List<T> methodAnnotations;

	private final List<T> allAnnotations;

	public Annotations(DbUnitTestContext context, Class<? extends Annotation> container, Class<T> annotation) {

		this.classAnnotations = getClassAnnotations(context.getTestClass(), container, annotation);
		this.methodAnnotations = getMethodAnnotations(context.getTestMethod(), container, annotation);

		List<T> allAnnotations = new ArrayList<T>(this.classAnnotations.size() + this.methodAnnotations.size());
		allAnnotations.addAll(this.classAnnotations);
		allAnnotations.addAll(this.methodAnnotations);

		this.allAnnotations = Collections.unmodifiableList(allAnnotations);
	}

	/**
	 * Finds the annotations which have been declared at class level, on the class itself or any of its parents.
	 *
	 * @param element The class.
	 * @param container The type of container annotation to look for.
	 * @param annotation The type of annotation to look for.
	 * @return The list of annotations found on the class and its parents, can be empty but never {@code null}.
	 */
	private List<T> getClassAnnotations(Class<?> element, Class<? extends Annotation> container, Class<T> annotation) {

		List<T> annotations = new ArrayList<T>();
		addAnnotationToList(annotations, AnnotationUtils.findAnnotation(element, annotation));
		addRepeatableAnnotationsToList(annotations, AnnotationUtils.findAnnotation(element, container));

		return Collections.unmodifiableList(annotations);
	}

	/**
	 * Finds the annotations which have been declared at method level.
	 *
	 * @param element The method.
	 * @param container The type of container annotation to look for.
	 * @param annotation The type of annotation to look for.
	 * @return The list of annotations found on the method, can be empty but never {@code null}.
	 */
	private List<T> getMethodAnnotations(Method element, Class<? extends Annotation> container, Class<T> annotation) {

		List<T> annotations = new ArrayList<T>();
		addAnnotationToList(annotations, AnnotationUtils.findAnnotation(element, annotation));
		addRepeatableAnnotationsToList(annotations, AnnotationUtils.findAnnotation(element, container));

		return Collections.unmodifiableList(annotations);
	}

	private void addAnnotationToList(List<T> annotations, T annotation) {
		if (annotation != null) {
			annotations.add(annotation);
		}
	}

	@SuppressWarnings("unchecked")
	private void addRepeatableAnnotationsToList(List<T> annotations, Annotation container) {
		if (container != null) {
			T[] value = (T[]) AnnotationUtils.getValue(container);

			for (T annotation : value) {
				annotations.add(annotation);
			}
		}
	}

	public List<T> getClassAnnotations() {
		return this.classAnnotations;
	}

	public List<T> getMethodAnnotations() {
		return this.methodAnnotations;
	}

	public Iterator<T> iterator() {
		return this.allAnnotations.iterator();
	}

	public static <T extends Annotation> Annotations<T> get(DbUnitTestContext testContext,
			Class<? extends Annotation> container, Class<T> annotation) {
		return new Annotations<T>(testContext, container, annotation);
	}

}