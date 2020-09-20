//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.gradle.codegen.state;

import java.io.IOException;
import java.util.List;

import javax.lang.model.SourceVersion;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.sandpolis.gradle.codegen.ConfigExtension;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

/**
 * Generator for document bindings.
 *
 * @author cilki
 */
public abstract class STGenerator extends DefaultTask {

	public static final String ST_PREFIX = "Virt";

	public static final String ST_PACKAGE = "com.sandpolis.core.instance.state";

	protected List<DocumentSpec> flatTree;

	@TaskAction
	public void action() throws Exception {

		// Load the schema
		flatTree = new ObjectMapper().readValue(
				((ConfigExtension) getProject().getExtensions().getByName("codegen")).stateTree,
				new TypeReference<List<DocumentSpec>>() {
				});

		// Check tree preconditions
		flatTree.forEach(this::validateDocument);

		// Calculate module namespace
		int namespace = addTagType(Hashing.murmur3_32().hashBytes(getProject().getName().getBytes()).asInt(), 1);

		// Find root document
		flatTree.stream().filter(document -> document.parent != null).findAny().ifPresent(document -> {
			if (document.parent.isEmpty()) {
				processDocument(null, document, String.valueOf(namespace));
			} else {
				processDocument(null, document, String.valueOf(namespace) + "." + document.parent);
			}

		});
	}

	protected void writeClass(TypeSpec spec) {
		try {
			JavaFile.builder(getProject().getName() + ".state", spec)
					.addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
					.skipJavaLangImports(true).build().writeTo(getProject().file("gen/main/java"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Assert the validity the given document.
	 */
	public void validateDocument(DocumentSpec document) {

		// Name must be a valid Java identifier
		for (var component : document.name.split("\\.")) {
			if (!SourceVersion.isIdentifier(component))
				throw new RuntimeException("Invalid document name: " + document.name);
		}

		// Validate sub-attributes
		if (document.attributes != null) {
			for (var entry : document.attributes.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on attribute: " + entry.getValue());

				validateAttribute(entry.getValue());
			}
		}

		// Validate sub-documents
		if (document.documents != null) {
			for (var entry : document.documents.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on document: " + entry.getValue());

				// Ensure sub-document exists
				if (flatTree.stream().filter(d -> entry.getValue().equals(d.name)).findAny().isEmpty())
					throw new RuntimeException("Failed to find document: " + entry.getValue());
			}
		}

		// Validate sub-collections
		if (document.collections != null) {
			for (var entry : document.collections.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on collection: " + entry.getValue());
			}
		}

		// Validate sub-relations
		if (document.relations != null) {
			for (var entry : document.relations.entrySet()) {
				if (entry.getKey() == 0)
					throw new RuntimeException("Found invalid tag on relation: " + entry.getValue());
			}
		}
	}

	/**
	 * Assert the validity the given attribute.
	 */
	public void validateAttribute(AttributeSpec attribute) {

		// Name must be a valid Java identifier
		if (!SourceVersion.isIdentifier(attribute.name))
			throw new RuntimeException("Invalid attribute name: " + attribute.name);

		// Type must be present
		if (attribute.type == null || attribute.type.isEmpty())
			throw new RuntimeException("Missing type on attribute: " + attribute.name);
	}

	/**
	 * Generate the given attribute from the specification.
	 */
	public abstract void processAttribute(TypeSpec.Builder parent, AttributeSpec attribute, String oid);

	/**
	 * Generate the given collection from the specification.
	 */
	public abstract void processCollection(TypeSpec.Builder parent, DocumentSpec document, String oid);

	/**
	 * Generate the given document from the specification.
	 */
	public abstract void processDocument(TypeSpec.Builder parent, DocumentSpec document, String oid);

	/**
	 * Generate the given relation from the specification.
	 */
	public abstract void processRelation(TypeSpec.Builder parent, RelationSpec relation, String oid);

	protected static int addTagType(int tag, int type) {
		if (type > 3 || type < 0) {
			throw new IllegalArgumentException();
		}
		return (tag << 2) | type;
	}
}
