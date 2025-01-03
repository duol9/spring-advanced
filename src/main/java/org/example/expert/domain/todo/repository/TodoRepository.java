package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TodoRepository extends JpaRepository<Todo, Long> {

	@EntityGraph(attributePaths = {"user"})
	@Query("SELECT t FROM Todo t")
	Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

	int countById(Long todoId);
}
