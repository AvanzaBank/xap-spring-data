package org.springframework.data.xap.querydsl.predicate;

import com.mysema.query.types.Ops;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpression;
import com.mysema.query.types.expr.ComparableOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.xap.model.Person;
import org.springframework.data.xap.model.Team;
import org.springframework.data.xap.model.TeamStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.springframework.data.xap.model.QTeam.team;
import static org.springframework.data.xap.model.TeamStatus.INACTIVE;
import static org.springframework.data.xap.model.TeamStatus.UNKNOWN;
import static org.springframework.data.xap.querydsl.QChangeSet.changeSet;
import static org.springframework.data.xap.querydsl.QueryDslProjection.projection;

/**
 * @author Leonid_Poliakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PredicateQueryDslTest {
    private static final Person nick = new Person("1", "Nick", 25);
    private static final Person chris = new Person("2", "", 40);
    private static final Person paul = new Person("3", "Paul", 33);
    private static final Team itspecial = new Team("1", "itspecial", chris, 10, paul, TeamStatus.ACTIVE, currentDay(+1));
    private static final Team avolition = new Team("2", "avolition", nick, 50, null, INACTIVE, currentDay(-1));
    private static final Set<Team> allTeams = newHashSet(itspecial, avolition);

    @Autowired
    private PredicateTeamRepository repository;

    private static Date currentDay(int daysOffset) {
        return new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysOffset));
    }

    @Before
    public void setUp() {
        repository.save(asList(itspecial, avolition));
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll() {
        assertEquals(
                allTeams,
                unsorted(null)
        );
    }

    @Test
    public void testFindByFields() {
        // single field
        assertEquals(
                avolition,
                one(team.name.eq(avolition.getName()))
        );
        // two fields of one team
        assertEquals(
                avolition,
                one(team.id.eq(avolition.getId()).and(team.name.eq(avolition.getName())))
        );
        // two fields of different teams, and operator
        assertNull(
                one(team.id.eq(itspecial.getId()).and(team.name.eq(avolition.getName())))
        );
        // two fields of different teams, or operator
        assertEquals(
                allTeams,
                unsorted(team.name.eq(itspecial.getName()).or(team.name.eq(avolition.getName())))
        );
    }

    @Test
    public void testFindByBooleanOperations() {
        // not equals
        assertEquals(
                itspecial,
                one(team.id.ne(avolition.getId()))
        );
        // less than
        assertEquals(
                allTeams,
                unsorted(team.membersCount.lt(60))
        );
        // greater than
        assertEquals(
                avolition,
                one(team.membersCount.gt(40))
        );
        // less than or equals
        assertEquals(
                itspecial,
                one(team.membersCount.loe(itspecial.getMembersCount()))
        );
        // greater than or equals
        assertEquals(
                avolition,
                one(team.membersCount.goe(avolition.getMembersCount()))
        );
        // like
        assertEquals(
                avolition,
                one(team.name.like("%tion%"))
        );
        // is null
        assertEquals(
                avolition,
                one(team.sponsor.isNull())
        );
        // is not null
        assertEquals(
                itspecial,
                one(team.sponsor.isNotNull())
        );
        // in
        assertEquals(
                itspecial,
                one(team.membersCount.in(5, 10, 15))
        );
        // not in
        assertEquals(
                avolition,
                one(team.membersCount.notIn(5, 10, 15))
        );
        // between
        assertEquals(
                allTeams,
                unsorted(team.membersCount.between(5, 55))
        );
        // not between
        assertEquals(
                itspecial,
                one(team.membersCount.notBetween(40, 60))
        );
        // matches regex
        assertEquals(
                avolition,
                one(team.name.matches(".*tion"))
        );
        // is empty
        assertEquals(
                itspecial,
                one(team.leader.name.isEmpty())
        );
        // is not empty
        assertEquals(
                avolition,
                one(team.leader.name.isNotEmpty())
        );
    }

    @Test
    public void testFindByEmbeddedFields() {
        // leader name equals
        assertEquals(
                avolition,
                one(team.leader.name.eq(avolition.getLeader().getName()))
        );
        // sponsor name not equals (test if team with sponsor will be found)
        assertEquals(
                itspecial,
                one(team.sponsor.name.ne("whoami"))
        );
        // sponsor name not equals (test if team with null sponsor is ignored)
        assertNull(
                one(team.sponsor.name.ne(itspecial.getSponsor().getName()))
        );
    }

    @Test
    public void testFindByEnum() {
        // enum equals
        assertEquals(
                avolition,
                one(team.status.eq(INACTIVE))
        );
        // enum in
        assertEquals(
                avolition,
                one(team.status.in(INACTIVE, UNKNOWN))
        );
        // enum not equals
        assertEquals(
                allTeams,
                unsorted(team.status.ne(UNKNOWN))
        );
        // enum not null
        assertEquals(
                allTeams,
                unsorted(team.status.isNotNull())
        );
    }

    @Test
    public void testCount() {
        // count all
        assertEquals(
                allTeams.size(),
                repository.count(team.status.isNotNull())
        );
        // count by name
        assertEquals(
                1,
                repository.count(team.name.eq(avolition.getName()))
        );
    }

    @Test
    public void testFindAndSort() {
        // sort by name
        assertEquals(
                asList(avolition, itspecial),
                sorted(null, team.name.asc())
        );
        // sort by id desc
        assertEquals(
                asList(avolition, itspecial),
                sorted(null, team.id.desc())
        );
        // sort by embedded field (nulls first)
        assertEquals(
                asList(avolition, itspecial),
                sorted(null, team.sponsor.name.desc())
        );
    }

    @Test
    public void testPagination() {
        // sort by name
        Sort sort = new Sort(Sort.Direction.ASC, "name");
        // select pages of size 1 with sort
        Page<Team> firstPage = repository.findAll(null, new PageRequest(0, 1, sort));
        Page<Team> secondPage = repository.findAll(null, new PageRequest(1, 1, sort));
        Page<Team> thirdPage = repository.findAll(null, new PageRequest(2, 1, sort));
        // check pages
        assertEquals(asList(avolition), newArrayList(firstPage));
        assertEquals(asList(itspecial), newArrayList(secondPage));
        assertEquals(0, thirdPage.getSize());

        // paging without sort
        assertEquals(allTeams, newHashSet(repository.findAll(null, new PageRequest(0, 2))));
    }

    @Test
    public void testOperationPrecedence() {
        // expected: select A1 and A2 or B1 and B2 = {A,B}
        // query must result in "name = itspecial and id = 1 or name = avolition and id = 2"
        BooleanExpression itspecialClauses = team.name.eq(itspecial.getName()).and(team.id.eq(itspecial.getId()));
        BooleanExpression avolitionClauses = team.name.eq(avolition.getName()).and(team.id.eq(avolition.getId()));
        assertEquals(
                allTeams,
                unsorted(itspecialClauses.or(avolitionClauses))
        );

        // expected: select (A1 or B1) and (A2 or B2) = {A,B}
        // query must result in "(name = itspecial or name = avolition) and (id = 1 or id = 2)"
        BooleanExpression nameClauses = team.name.eq(itspecial.getName()).or(team.name.eq(avolition.getName()));
        BooleanExpression idClauses = team.id.eq(itspecial.getId()).or(team.id.eq(avolition.getId()));
        assertEquals(
                allTeams,
                unsorted(nameClauses.and(idClauses))
        );
    }

    @Test
    public void testFindOneWithProjection() {
        Team foundTeam = repository.findOne(team.name.eq(avolition.getName()), projection(team.name, team.leader.age));
        assertEquals(avolition.getName(), foundTeam.getName());
        assertNull(foundTeam.getId());
        assertNotNull(foundTeam.getLeader());
        assertNull(foundTeam.getLeader().getId());
        assertNotNull(foundTeam.getLeader().getAge());
    }

    @Test
    public void testFindAllWithProjection() {
        Predicate allPredicate = null;
        Set<Team> foundTeams = newHashSet(repository.findAll(allPredicate, projection(team.name)));
        assertEquals(2, foundTeams.size());
        for (Team team : foundTeams) {
            assertNotNull(team.getName());
            assertNull(team.getId());
        }
    }

    @Test
    public void testFindAllWithOrderAndProjection() {
        Predicate allPredicate = null;
        List<Team> foundTeams = newArrayList(repository.findAll(allPredicate, projection(team.name), team.name.asc()));
        assertEquals(2, foundTeams.size());
        for (Team team : foundTeams) {
            assertNotNull(team.getName());
            assertNull(team.getId());
        }
        assertEquals(avolition.getName(), foundTeams.get(0).getName());
        assertEquals(itspecial.getName(), foundTeams.get(1).getName());
    }

    @Test
    public void testFindAllWithPagingAndProjection() {
        PageRequest page = new PageRequest(0, 1, new Sort(Sort.Direction.ASC, "name"));
        List<Team> foundTeams = newArrayList(repository.findAll(team.name.isNotNull(), page, projection(team.name)));
        assertEquals(1, foundTeams.size());
        assertNotNull(foundTeams.get(0).getName());
        assertNull(foundTeams.get(0).getId());
        assertEquals(avolition.getName(), foundTeams.get(0).getName());
    }

    @Test
    public void testFindWithSysdate() {
        // before sysdate
        assertEquals(
                avolition,
                one(team.creationDate.lt(sysdate()))
        );

        // after sysdate
        assertEquals(
                itspecial,
                one(team.creationDate.gt(sysdate()))
        );
    }

    @Test
    public void testChange() {
        repository.change(
                team.name.eq(avolition.getName()),
                changeSet().increment(team.leader.age, 5).unset(team.status)
        );

        Team updated = repository.findOne(team.name.eq(avolition.getName()));
        assertEquals(avolition.getLeader().getAge() + 5, updated.getLeader().getAge().intValue());
        assertNull(updated.getStatus());
    }

    @Test
    public void testStringComparison() {
        Team weirdo = new Team("3", "weir}DO", null, null, null, TeamStatus.UNKNOWN, null);
        repository.save(weirdo);

        // contains
        assertEquals(
                weirdo,
                one(team.name.contains("r}D"))
        );
        assertEquals(
                weirdo,
                one(team.name.containsIgnoreCase("IR}do"))
        );

        // starts with
        assertEquals(
                avolition,
                one(team.name.startsWith("avo"))
        );
        assertEquals(
                weirdo,
                one(team.name.startsWithIgnoreCase("WeiR}d"))
        );

        // ends with
        assertEquals(
                avolition,
                one(team.name.endsWith("tion"))
        );
        assertEquals(
                weirdo,
                one(team.name.endsWithIgnoreCase("R}do"))
        );
    }

    @Test
    public void testTakeOne() {
        Predicate predicate = team.name.eq(avolition.getName());
        assertEquals(
                avolition,
                repository.takeOne(predicate)
        );
        assertEquals(0, repository.count(predicate));
        assertEquals(1, repository.count());
    }

    @Test
    public void testTakeAll() {
        Predicate predicate = team.name.isNotEmpty();
        assertEquals(
                allTeams,
                newHashSet(repository.takeAll(predicate))
        );
        assertEquals(0, repository.count(predicate));
        assertEquals(0, repository.count());
    }

    @Test
    public void testTakeOneProjection() {
        Predicate predicate = team.name.eq(avolition.getName());
        Team result = repository.takeOne(predicate, projection(team.name));
        assertEquals(avolition.getName(), result.getName());
        assertNull(result.getId());
        assertNull(result.getStatus());

        assertEquals(0, repository.count(predicate));
        assertEquals(1, repository.count());
    }

    @Test
    public void testTakeAllProjection() {
        Predicate predicate = team.name.isNotNull();
        for (Team result : repository.takeAll(predicate, projection(team.name))) {
            assertNotNull(result.getName());
            assertNull(result.getId());
            assertNull(result.getStatus());
        }

        assertEquals(0, repository.count(predicate));
        assertEquals(0, repository.count());
    }

    private Team one(Predicate predicate) {
        return repository.findOne(predicate);
    }

    private List<Team> sorted(Predicate predicate, OrderSpecifier<?>... orders) {
        return newArrayList(repository.findAll(predicate, orders));
    }

    private Set<Team> unsorted(Predicate predicate) {
        return newHashSet(repository.findAll(predicate));
    }

    private ComparableExpression<Date> sysdate() {
        return ComparableOperation.create(Date.class, Ops.DateTimeOps.SYSDATE);
    }

}