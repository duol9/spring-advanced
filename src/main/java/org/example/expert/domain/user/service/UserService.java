package org.example.expert.domain.user.service;

import lombok.RequiredArgsConstructor;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserResponse getUser(long userId) {
		User user = findUserOrThrow(userId);
		return new UserResponse(user.getId(), user.getEmail());
	}

	@Transactional
	public void changePassword(long userId, UserChangePasswordRequest userChangePasswordRequest) {
		User user = findUserOrThrow(userId);

		if (passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), user.getPassword())) {
			throw new InvalidRequestException("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.");
		}

		if (!passwordEncoder.matches(userChangePasswordRequest.getOldPassword(), user.getPassword())) {
			throw new InvalidRequestException("잘못된 비밀번호입니다.");
		}

		user.changePassword(passwordEncoder.encode(userChangePasswordRequest.getNewPassword()));
	}

	public User findUserOrThrow(long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new InvalidRequestException("User not found"));
	}
}
