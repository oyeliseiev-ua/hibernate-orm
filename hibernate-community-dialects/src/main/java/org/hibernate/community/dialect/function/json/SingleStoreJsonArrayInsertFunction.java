/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.AbstractJsonArrayInsertFunction;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * SingleStore json_array_insert function.
 */
public class SingleStoreJsonArrayInsertFunction extends AbstractJsonArrayInsertFunction {

	public SingleStoreJsonArrayInsertFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( translator.getLiteralValue(
				jsonPath ) );
		final SqlAstNode value = arguments.get( 2 );
		final int arrayIndex = getArrayIndex( jsonPathElements );
		sqlAppender.appendSql( "case when json_get_type(json_extract_json(" );
		json.accept( translator );
		buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
		sqlAppender.appendSql( ")) = 'array' THEN " );
		if ( jsonPathElements.size() > 1 ) {
			sqlAppender.appendSql( "json_set_json(" );
			json.accept( translator );
			buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
			sqlAppender.appendSql( ", " );
		}
		sqlAppender.appendSql( "json_splice_json(" );
		sqlAppender.appendSql( "json_extract_json(" );
		json.accept( translator );
		buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
		sqlAppender.appendSql( "), " );
		sqlAppender.appendSql( arrayIndex );
		sqlAppender.appendSql( ", 0, " );
		sqlAppender.appendSql( "to_json(" );
		value.accept( translator );
		sqlAppender.appendSql( "))" );
		if ( jsonPathElements.size() > 1 ) {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( " else " );
		json.accept( translator );
		sqlAppender.appendSql( " END" );
	}

	private static int getArrayIndex(List<JsonPathHelper.JsonPathElement> jsonPathElements) {
		if ( jsonPathElements.isEmpty() ) {
			throw new QueryException( "SingleStore json_array_insert function requires at least one json path element" );
		}
		JsonPathHelper.JsonPathElement lastPathElement = jsonPathElements.get( jsonPathElements.size() - 1 );
		if ( !( lastPathElement instanceof JsonPathHelper.JsonIndexAccess ) ) {
			throw new QueryException(
					"SingleStore json_array_insert function last path parameter must be an array index element" );
		}
		return ( (JsonPathHelper.JsonIndexAccess) lastPathElement ).index();
	}

	private static void buildJsonPath(
			SqlAppender sqlAppender, Expression jsonPath, List<JsonPathHelper.JsonPathElement> jsonPathElements) {
		for ( int i = 0; i < jsonPathElements.size() - 1; i++ ) {
			JsonPathHelper.JsonPathElement pathElement = jsonPathElements.get( i );
			sqlAppender.appendSql( ',' );
			if ( pathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
			}
			else if ( pathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
				final String parameterName = ( (JsonPathHelper.JsonParameterIndexAccess) pathElement ).parameterName();
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			else {
				sqlAppender.appendSql( '\'' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) pathElement ).index() );
				sqlAppender.appendSql( '\'' );
			}
		}
	}
}
