package org.springframework.data.xap.examples.advanced.projection;

import org.springframework.data.xap.examples.model.MeetingRoom;
import org.springframework.data.xap.examples.model.Person;
import org.springframework.data.xap.querydsl.XapQueryDslPredicateExecutor;
import org.springframework.data.xap.repository.XapRepository;

/**
 * @author Anna_Babich.
 */
public interface PredicateRoomRepository  extends XapRepository<MeetingRoom, String>, XapQueryDslPredicateExecutor<MeetingRoom> {
}