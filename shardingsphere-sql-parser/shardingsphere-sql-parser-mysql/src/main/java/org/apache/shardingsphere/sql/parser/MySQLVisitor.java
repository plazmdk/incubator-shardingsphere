/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sql.parser;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.shardingsphere.sql.parser.api.SQLVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementBaseVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AggregationFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.BitExprContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.BooleanLiteralsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.BooleanPrimaryContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.CastFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.CharFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ColumnNameContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ColumnNamesContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ConvertFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.DataTypeName_Context;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ExprContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ExtractFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.FunctionCallContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.GroupConcatFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.IdentifierContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.IndexNameContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.IntervalExpressionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.LiteralsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.NumberLiteralsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.OrderByClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.OrderByItemContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.OwnerContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ParameterMarkerContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.PositionFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.PredicateContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.RegularFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SchemaNameContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SimpleExprContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SpecialFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.StringLiteralsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SubstringFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableNameContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableNamesContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.UnreservedWord_Context;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.WeightStringFunctionContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.WindowFunctionContext;
import org.apache.shardingsphere.sql.parser.core.constant.AggregationType;
import org.apache.shardingsphere.sql.parser.core.constant.LogicalOperator;
import org.apache.shardingsphere.sql.parser.core.constant.OrderDirection;
import org.apache.shardingsphere.sql.parser.sql.ASTNode;
import org.apache.shardingsphere.sql.parser.sql.segment.ddl.index.IndexSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.InsertColumnsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.complex.CommonExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.complex.SubquerySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.AggregationDistinctProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.AggregationProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.OrderBySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.ExpressionOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.OrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.OrPredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateBetweenRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateInRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.SchemaSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.value.BooleanValue;
import org.apache.shardingsphere.sql.parser.sql.value.ListValue;
import org.apache.shardingsphere.sql.parser.sql.value.LiteralValue;
import org.apache.shardingsphere.sql.parser.sql.value.NumberValue;
import org.apache.shardingsphere.sql.parser.sql.value.ParameterMarkerValue;
import org.apache.shardingsphere.sql.parser.util.SQLUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * MySQL visitor.
 *
 * @author panjuan
 */
public class MySQLVisitor extends MySQLStatementBaseVisitor<ASTNode> implements SQLVisitor {
    
    @Getter(AccessLevel.PROTECTED)
    private int currentParameterIndex;
    
    @Override
    public ASTNode visitSchemaName(final SchemaNameContext ctx) {
        return visit(ctx.identifier());
    }
    
    @Override
    public ASTNode visitTableNames(final TableNamesContext ctx) {
        ListValue<TableSegment> result = new ListValue<>(new LinkedList<TableSegment>());
        for (TableNameContext each : ctx.tableName()) {
            result.getValues().add((TableSegment) visit(each));
        }
        return result;
    }
    
    @Override
    public ASTNode visitTableName(final TableNameContext ctx) {
        LiteralValue tableName = (LiteralValue) visit(ctx.name());
        TableSegment result = new TableSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), tableName.getLiteral());
        OwnerContext owner = ctx.owner();
        if (null != owner) {
            result.setOwner(createSchemaSegment(owner));
        }
        return result;
    }
    
    @Override
    public ASTNode visitColumnNames(final ColumnNamesContext ctx) {
        Collection<ColumnSegment> segments = new LinkedList<>();
        for (ColumnNameContext each : ctx.columnName()) {
            segments.add((ColumnSegment) visit(each));
        }
        InsertColumnsSegment result = new InsertColumnsSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex());
        result.getColumns().addAll(segments);
        return result;
    }
    
    @Override
    public ASTNode visitColumnName(final ColumnNameContext ctx) {
        LiteralValue columnName = (LiteralValue) visit(ctx.name());
        ColumnSegment result = new ColumnSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), columnName.getLiteral());
        OwnerContext owner = ctx.owner();
        if (null != owner) {
            result.setOwner(createTableSegment(owner));
        }
        return result;
    }
    
    @Override
    public ASTNode visitIndexName(final IndexNameContext ctx) {
        LiteralValue indexName = (LiteralValue) visit(ctx.identifier());
        return new IndexSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), indexName.getLiteral());
    }

    @Override
    public ASTNode visitDataTypeName_(final DataTypeName_Context ctx) {
        return visit(ctx.identifier(0));
    }

    @Override
    public ASTNode visitExpr(final ExprContext ctx) {
        BooleanPrimaryContext bool = ctx.booleanPrimary();
        if (null != bool) {
            return visit(bool);
        } else if (null != ctx.logicalOperator()) {
            return mergePredicateSegment(visit(ctx.expr(0)), visit(ctx.expr(1)), ctx.logicalOperator().getText());
        } else if (!ctx.expr().isEmpty()) {
            return visit(ctx.expr(0));
        }
        return createExpressionSegment(new LiteralValue(ctx.getText()), ctx);
    }
    
    @Override
    public ASTNode visitBooleanPrimary(final BooleanPrimaryContext ctx) {
        if (null != ctx.subquery()) {
            return new SubquerySegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.subquery().getText());
        }
        if (null != ctx.comparisonOperator()) {
            return createCompareSegment(ctx);
        }
        if (null != ctx.predicate()) {
            return visit(ctx.predicate());
        }
        return createExpressionSegment(new LiteralValue(ctx.getText()), ctx);
    }
    
    @Override
    public ASTNode visitPredicate(final PredicateContext ctx) {
        if (null != ctx.subquery()) {
            return new SubquerySegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.subquery().getText());
        }
        if (null != ctx.IN()) {
            return createInSegment(ctx);
        }
        if (null != ctx.BETWEEN()) {
            return createBetweenSegment(ctx);
        }
        BitExprContext bitExpr = ctx.bitExpr(0);
        if (null != bitExpr) {
            return createExpressionSegment(visit(bitExpr), ctx);
        }
        return createExpressionSegment(new LiteralValue(ctx.getText()), ctx);
    }
    
    @Override
    public ASTNode visitBitExpr(final BitExprContext ctx) {
        SimpleExprContext simple = ctx.simpleExpr();
        if (null != simple) {
            return visit(simple);
        }
        return new LiteralValue(ctx.getText());
    }
    
    @Override
    public ASTNode visitSimpleExpr(final SimpleExprContext ctx) {
        if (null != ctx.subquery()) {
            return new SubquerySegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.subquery().getText());
        }
        if (null != ctx.parameterMarker()) {
            return visit(ctx.parameterMarker());
        }
        if (null != ctx.literals()) {
            return visit(ctx.literals());
        }
        if (null != ctx.intervalExpression()) {
            return visit(ctx.intervalExpression());
        }
        if (null != ctx.functionCall()) {
            return visit(ctx.functionCall());
        }
        if (null != ctx.columnName()) {
            return visit(ctx.columnName());
        }
        return new CommonExpressionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitParameterMarker(final ParameterMarkerContext ctx) {
        return new ParameterMarkerValue(currentParameterIndex++);
    }
    
    @Override
    public ASTNode visitLiterals(final LiteralsContext ctx) {
        if (null != ctx.stringLiterals()) {
            return visit(ctx.stringLiterals());
        }
        if (null != ctx.numberLiterals()) {
            return visit(ctx.numberLiterals());
        }
        if (null != ctx.booleanLiterals()) {
            return visit(ctx.booleanLiterals());
        }
        if (null != ctx.nullValueLiterals()) {
            return new CommonExpressionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
        }
        return new LiteralValue(ctx.getText());
    }
    
    @Override
    public ASTNode visitStringLiterals(final StringLiteralsContext ctx) {
        String text = ctx.getText();
        return new LiteralValue(text.substring(1, text.length() - 1));
    }
    
    @Override
    public ASTNode visitNumberLiterals(final NumberLiteralsContext ctx) {
        return new NumberValue(ctx.getText());
    }
    
    @Override
    public ASTNode visitBooleanLiterals(final BooleanLiteralsContext ctx) {
        return new BooleanValue(ctx.getText());
    }
    
    @Override
    public ASTNode visitIntervalExpression(final IntervalExpressionContext ctx) {
        calculateParameterCount(Collections.singleton(ctx.expr()));
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitOrderByClause(final OrderByClauseContext ctx) {
        Collection<OrderByItemSegment> items = new LinkedList<>();
        for (OrderByItemContext each : ctx.orderByItem()) {
            items.add((OrderByItemSegment) visit(each));
        }
        return new OrderBySegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), items);
    }
    
    @Override
    public ASTNode visitOrderByItem(final OrderByItemContext ctx) {
        OrderDirection orderDirection = null != ctx.DESC() ? OrderDirection.DESC : OrderDirection.ASC;
        if (null != ctx.columnName()) {
            ColumnSegment column = (ColumnSegment) visit(ctx.columnName());
            return new ColumnOrderByItemSegment(column, orderDirection);
        }
        if (null != ctx.numberLiterals()) {
            return new IndexOrderByItemSegment(ctx.numberLiterals().getStart().getStartIndex(), ctx.numberLiterals().getStop().getStopIndex(),
                    SQLUtil.getExactlyNumber(ctx.numberLiterals().getText(), 10).intValue(), orderDirection);
        }
        return new ExpressionOrderByItemSegment(ctx.expr().getStart().getStartIndex(), ctx.expr().getStop().getStopIndex(), ctx.expr().getText(), orderDirection);
    }
    
    @Override
    public ASTNode visitFunctionCall(final FunctionCallContext ctx) {
        if (null != ctx.aggregationFunction()) {
            return visit(ctx.aggregationFunction());
        }
        if (null != ctx.regularFunction()) {
            return visit(ctx.regularFunction());
        }
        if (null != ctx.specialFunction()) {
            return visit(ctx.specialFunction());
        }
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitAggregationFunction(final AggregationFunctionContext ctx) {
        if (AggregationType.isAggregationType(ctx.aggregationFunctionName_().getText())) {
            return createAggregationSegment(ctx);
        }
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitSpecialFunction(final SpecialFunctionContext ctx) {
        if (null != ctx.groupConcatFunction()) {
            return visit(ctx.groupConcatFunction());
        }
        if (null != ctx.windowFunction()) {
            return visit(ctx.windowFunction());
        }
        if (null != ctx.castFunction()) {
            return visit(ctx.castFunction());
        }
        if (null != ctx.convertFunction()) {
            return visit(ctx.convertFunction());
        }
        if (null != ctx.positionFunction()) {
            return visit(ctx.positionFunction());
        }
        if (null != ctx.substringFunction()) {
            return visit(ctx.substringFunction());
        }
        if (null != ctx.extractFunction()) {
            return visit(ctx.extractFunction());
        }
        if (null != ctx.charFunction()) {
            return visit(ctx.charFunction());
        }
        if (null != ctx.weightStringFunction()) {
            return visit(ctx.weightStringFunction());
        }
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitGroupConcatFunction(final GroupConcatFunctionContext ctx) {
        calculateParameterCount(ctx.expr());
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitWindowFunction(final WindowFunctionContext ctx) {
        calculateParameterCount(ctx.expr());
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitCastFunction(final CastFunctionContext ctx) {
        calculateParameterCount(Collections.singleton(ctx.expr()));
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitConvertFunction(final ConvertFunctionContext ctx) {
        calculateParameterCount(Collections.singleton(ctx.expr()));
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitPositionFunction(final PositionFunctionContext ctx) {
        calculateParameterCount(ctx.expr());
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitSubstringFunction(final SubstringFunctionContext ctx) {
        calculateParameterCount(Collections.singleton(ctx.expr()));
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitExtractFunction(final ExtractFunctionContext ctx) {
        calculateParameterCount(Collections.singleton(ctx.expr()));
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitCharFunction(final CharFunctionContext ctx) {
        calculateParameterCount(ctx.expr());
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitWeightStringFunction(final WeightStringFunctionContext ctx) {
        calculateParameterCount(Collections.singleton(ctx.expr()));
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitRegularFunction(final RegularFunctionContext ctx) {
        calculateParameterCount(ctx.expr());
        return new ExpressionProjectionSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), ctx.getText());
    }
    
    @Override
    public ASTNode visitIdentifier(final IdentifierContext ctx) {
        UnreservedWord_Context unreservedWord = ctx.unreservedWord_();
        if (null != unreservedWord) {
            return visit(unreservedWord);
        }
        return new LiteralValue(ctx.getText());
    }
    
    @Override
    public ASTNode visitUnreservedWord_(final UnreservedWord_Context ctx) {
        return new LiteralValue(ctx.getText());
    }
    
    // Segments
    private SchemaSegment createSchemaSegment(final OwnerContext ownerContext) {
        LiteralValue literalValue = (LiteralValue) visit(ownerContext.identifier());
        return new SchemaSegment(ownerContext.getStart().getStartIndex(), ownerContext.getStop().getStopIndex(), literalValue.getLiteral());
    }
    
    private TableSegment createTableSegment(final OwnerContext ownerContext) {
        LiteralValue literalValue = (LiteralValue) visit(ownerContext.identifier());
        return new TableSegment(ownerContext.getStart().getStartIndex(), ownerContext.getStop().getStopIndex(), literalValue.getLiteral());
    }
    
    private ASTNode createExpressionSegment(final ASTNode astNode, final ParserRuleContext context) {
        if (astNode instanceof LiteralValue) {
            return new LiteralExpressionSegment(context.start.getStartIndex(), context.stop.getStopIndex(), ((LiteralValue) astNode).getLiteral());
        }
        if (astNode instanceof NumberValue) {
            return new LiteralExpressionSegment(context.start.getStartIndex(), context.stop.getStopIndex(), ((NumberValue) astNode).getNumber());
        }
        if (astNode instanceof ParameterMarkerValue) {
            return new ParameterMarkerExpressionSegment(context.start.getStartIndex(), context.stop.getStopIndex(), ((ParameterMarkerValue) astNode).getParameterIndex());
        }
        return astNode;
    }
    
    private ASTNode createAggregationSegment(final AggregationFunctionContext ctx) {
        AggregationType type = AggregationType.valueOf(ctx.aggregationFunctionName_().getText());
        int innerExpressionStartIndex = ((TerminalNode) ctx.getChild(1)).getSymbol().getStartIndex();
        if (null != ctx.distinct()) {
            return new AggregationDistinctProjectionSegment(ctx.getStart().getStartIndex(),
                    ctx.getStop().getStopIndex(), ctx.getText(), type, innerExpressionStartIndex, getDistinctExpression(ctx));
        }
        return new AggregationProjectionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(),
                ctx.getText(), type, innerExpressionStartIndex);
    }
    
    private String getDistinctExpression(final AggregationFunctionContext ctx) {
        StringBuilder result = new StringBuilder();
        for (int i = 3; i < ctx.getChildCount() - 1; i++) {
            result.append(ctx.getChild(i).getText());
        }
        return result.toString();
    }
    
    private PredicateSegment createCompareSegment(final BooleanPrimaryContext ctx) {
        ASTNode leftValue = visit(ctx.booleanPrimary());
        ASTNode rightValue = visit(ctx.predicate());
        if (rightValue instanceof ColumnSegment) {
            return new PredicateSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), (ColumnSegment) leftValue, (ColumnSegment) rightValue);
        }
        return new PredicateSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(),
                (ColumnSegment) leftValue, new PredicateCompareRightValue(ctx.comparisonOperator().getText(), (ExpressionSegment) rightValue));
    }
    
    private PredicateSegment createInSegment(final PredicateContext ctx) {
        ColumnSegment column = (ColumnSegment) visit(ctx.bitExpr(0));
        Collection<ExpressionSegment> segments = Lists.transform(ctx.expr(), new Function<ExprContext, ExpressionSegment>() {
            
            @Override
            public ExpressionSegment apply(final ExprContext input) {
                return (ExpressionSegment) visit(input);
            }
        });
        return new PredicateSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), column, new PredicateInRightValue(segments));
    }
    
    private PredicateSegment createBetweenSegment(final PredicateContext ctx) {
        ColumnSegment column = (ColumnSegment) visit(ctx.bitExpr(0));
        ExpressionSegment between = (ExpressionSegment) visit(ctx.bitExpr(1));
        ExpressionSegment and = (ExpressionSegment) visit(ctx.predicate());
        return new PredicateSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), column, new PredicateBetweenRightValue(between, and));
    }
    
    private OrPredicateSegment mergePredicateSegment(final ASTNode left, final ASTNode right, final String operator) {
        Optional<LogicalOperator> logicalOperator = LogicalOperator.valueFrom(operator);
        Preconditions.checkState(logicalOperator.isPresent());
        if (LogicalOperator.OR == logicalOperator.get()) {
            return mergeOrPredicateSegment(left, right);
        }
        return mergeAndPredicateSegment(left, right);
    }
    
    private OrPredicateSegment mergeOrPredicateSegment(final ASTNode left, final ASTNode right) {
        OrPredicateSegment result = new OrPredicateSegment();
        result.getAndPredicates().addAll(getAndPredicates(left));
        result.getAndPredicates().addAll(getAndPredicates(right));
        return result;
    }
    
    private OrPredicateSegment mergeAndPredicateSegment(final ASTNode left, final ASTNode right) {
        OrPredicateSegment result = new OrPredicateSegment();
        for (AndPredicate eachLeft : getAndPredicates(left)) {
            for (AndPredicate eachRight : getAndPredicates(right)) {
                result.getAndPredicates().add(createAndPredicate(eachLeft, eachRight));
            }
        }
        return result;
    }
    
    private AndPredicate createAndPredicate(final AndPredicate left, final AndPredicate right) {
        AndPredicate result = new AndPredicate();
        result.getPredicates().addAll(left.getPredicates());
        result.getPredicates().addAll(right.getPredicates());
        return result;
    }
    
    private Collection<AndPredicate> getAndPredicates(final ASTNode astNode) {
        if (astNode instanceof OrPredicateSegment) {
            return ((OrPredicateSegment) astNode).getAndPredicates();
        }
        if (astNode instanceof AndPredicate) {
            return Collections.singleton((AndPredicate) astNode);
        }
        AndPredicate andPredicate = new AndPredicate();
        andPredicate.getPredicates().add((PredicateSegment) astNode);
        return Collections.singleton(andPredicate);
    }
    
    // TODO :FIXME, sql case id: insert_with_str_to_date
    private void calculateParameterCount(final Collection<ExprContext> exprContexts) {
        for (ExprContext each : exprContexts) {
            visit(each);
        }
    }
}
