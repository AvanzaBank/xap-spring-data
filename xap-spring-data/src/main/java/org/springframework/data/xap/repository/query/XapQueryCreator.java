package org.springframework.data.xap.repository.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.Iterator;

/**
 * @author Anna_Babich.
 */
public class XapQueryCreator extends AbstractQueryCreator<String, QueryBuilder> {
    private Pageable pageable;

    public XapQueryCreator(PartTree tree, ParameterAccessor parameters) {
        super(tree, parameters);
        pageable = parameters.getPageable();
    }

    public XapQueryCreator(PartTree tree) {
        super(tree);
    }

    @Override
    protected QueryBuilder create(Part part, Iterator iterator) {
        return new QueryBuilder(part);
    }

    @Override
    protected QueryBuilder and(Part part, QueryBuilder base, Iterator iterator) {
        return base.and(new QueryBuilder(part));
    }

    @Override
    protected QueryBuilder or(QueryBuilder base, QueryBuilder criteria) {
        return base.or(criteria);
    }

    @Override
    protected String complete(QueryBuilder criteria, Sort sort) {
        return criteria.page(pageable).sort(sort).buildQuery();
    }

}
