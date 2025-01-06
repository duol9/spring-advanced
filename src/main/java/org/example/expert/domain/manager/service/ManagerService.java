package org.example.expert.domain.manager.service;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.domain.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

	private final ManagerRepository managerRepository;
	private final TodoService todoService;
	private final UserService userService;

	@Transactional
	public ManagerSaveResponse saveManager(AuthUser authUser, long todoId, ManagerSaveRequest managerSaveRequest) {
		User user = User.fromAuthUser(authUser);
		Todo todo = todoService.findTodoOrThrow(todoId);

		if (todo.getUser() == null || !ObjectUtils.nullSafeEquals(user.getId(), todo.getUser().getId())) {
			throw new InvalidRequestException("해당 일정을 만든 유저가 유효하지 않습니다.");
		}
		// 작성자 검증
		//todoService.validateTodoOwner(user,todo);

		// 담당자로 지정된 유저 조회 후 검증
		User managerUser = userService.findUserOrThrow(managerSaveRequest.getManagerUserId());

		if (ObjectUtils.nullSafeEquals(user.getId(), managerUser.getId())) {
			throw new InvalidRequestException("일정 작성자는 본인을 담당자로 등록할 수 없습니다.");
		}

		Manager newManagerUser = new Manager(managerUser, todo);
		Manager savedManagerUser = managerRepository.save(newManagerUser);

		return new ManagerSaveResponse(
			savedManagerUser.getId(),
			new UserResponse(managerUser.getId(), managerUser.getEmail())
		);
	}

	public List<ManagerResponse> getManagers(long todoId) {
		Todo todo = todoService.findTodoOrThrow(todoId);

		List<Manager> managerList = managerRepository.findAllByTodoId(todo.getId());

		List<ManagerResponse> dtoList = new ArrayList<>();
		for (Manager manager : managerList) {
			User user = manager.getUser();
			dtoList.add(new ManagerResponse(
				manager.getId(),
				new UserResponse(user.getId(), user.getEmail())
			));
		}
		return dtoList;
	}

	@Transactional
	public void deleteManager(long userId, long todoId, long managerId) {

		User user = userService.findUserOrThrow(userId);
		Todo todo = todoService.findTodoOrThrow(todoId);

		// 작성자 검증
		todoService.validateTodoOwner(user,todo);

		// 삭제할 담당자 조회 후 검증
		Manager manager = findManagerOrThrow(managerId);

		if (!ObjectUtils.nullSafeEquals(todo.getId(), manager.getTodo().getId())) {
			throw new InvalidRequestException("해당 일정에 등록된 담당자가 아닙니다.");
		}

		managerRepository.delete(manager);
	}

	public Manager findManagerOrThrow(long managerId) {
		return managerRepository.findById(managerId)
			.orElseThrow(() -> new InvalidRequestException("Manager not found"));
	}
}
