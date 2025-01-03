package org.example.expert.domain.todo.service;

import lombok.RequiredArgsConstructor;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

	private final TodoRepository todoRepository;
	private final WeatherClient weatherClient;

	@Transactional
	public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
		User user = User.fromAuthUser(authUser);

		String weather = weatherClient.getTodayWeather();

		Todo newTodo = new Todo(
			todoSaveRequest.getTitle(),
			todoSaveRequest.getContents(),
			weather,
			user
		);
		Todo savedTodo = todoRepository.save(newTodo);

		return new TodoSaveResponse(
			savedTodo.getId(),
			savedTodo.getTitle(),
			savedTodo.getContents(),
			weather,
			new UserResponse(user.getId(), user.getEmail())
		);
	}

	public Page<TodoResponse> getTodos(int page, int size) {
		Pageable pageable = PageRequest.of(page - 1, size);

		Page<Todo> todos = todoRepository.findAllByOrderByModifiedAtDesc(pageable);

		return todos.map(todo -> new TodoResponse(
			todo.getId(),
			todo.getTitle(),
			todo.getContents(),
			todo.getWeather(),
			new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
			todo.getCreatedAt(),
			todo.getModifiedAt()
		));
	}

	public TodoResponse getTodo(long todoId) {
		Todo todo = findTodoOrThrow(todoId);

		User user = todo.getUser();

		return new TodoResponse(
			todo.getId(),
			todo.getTitle(),
			todo.getContents(),
			todo.getWeather(),
			new UserResponse(user.getId(), user.getEmail()),
			todo.getCreatedAt(),
			todo.getModifiedAt()
		);
	}

	public Todo findTodoOrThrow(long todoId) {
		return todoRepository.findById(todoId)
			.orElseThrow(() -> new InvalidRequestException("Todo not found"));
	}

	public void validateTodoOwner(User user, Todo todo) {
		if (todo.getUser() == null || !ObjectUtils.nullSafeEquals(user.getId(), todo.getUser().getId())) {
			throw new InvalidRequestException("해당 일정을 만든 유저가 유효하지 않습니다.");
		}
	}
}
