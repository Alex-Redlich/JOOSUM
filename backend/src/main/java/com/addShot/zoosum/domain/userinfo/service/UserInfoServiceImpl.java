package com.addShot.zoosum.domain.userinfo.service;

import com.addShot.zoosum.domain.activity.repository.ActivityRepository;
import com.addShot.zoosum.domain.animal.repository.AnimalMotionRepository;
import com.addShot.zoosum.domain.animal.repository.UserAnimalRepository;
import com.addShot.zoosum.domain.common.repository.BadgeRepository;
import com.addShot.zoosum.domain.common.repository.UserBadgeRepository;
import com.addShot.zoosum.domain.item.repository.ItemRepository;
import com.addShot.zoosum.domain.item.repository.UserItemRepository;
import com.addShot.zoosum.domain.user.repository.UserRepository;
import com.addShot.zoosum.domain.userinfo.dto.request.TreeCampaignRequest;
import com.addShot.zoosum.domain.userinfo.dto.response.BadgeListItemResponse;
import com.addShot.zoosum.domain.userinfo.dto.response.MainInfoResponse;
import com.addShot.zoosum.domain.userinfo.dto.response.MainResponse;
import com.addShot.zoosum.domain.userinfo.dto.response.PlogRecordResponse;
import com.addShot.zoosum.domain.userinfo.dto.response.SelectedAnimalResponse;
import com.addShot.zoosum.domain.userinfo.repository.UserPlogInfoRepository;
import com.addShot.zoosum.entity.ActivityHistory;
import com.addShot.zoosum.entity.Animal;
import com.addShot.zoosum.entity.AnimalMotion;
import com.addShot.zoosum.entity.Badge;
import com.addShot.zoosum.entity.User;
import com.addShot.zoosum.entity.UserAnimal;
import com.addShot.zoosum.entity.UserBadge;
import com.addShot.zoosum.entity.UserItem;
import com.addShot.zoosum.entity.UserPlogInfo;
import com.addShot.zoosum.entity.embedded.DivideTime;
import com.addShot.zoosum.entity.embedded.Mission;
import com.addShot.zoosum.entity.embedded.Time;
import com.addShot.zoosum.entity.embedded.Tree;
import com.addShot.zoosum.entity.embedded.UserBadgeId;
import com.addShot.zoosum.entity.embedded.UserPlogInfoId;
import com.addShot.zoosum.entity.enums.ActivityType;
import com.addShot.zoosum.entity.enums.CustomErrorType;
import com.addShot.zoosum.entity.enums.ItemType;
import com.addShot.zoosum.util.DistanceUtil;
import com.addShot.zoosum.util.RandomUtil;
import com.addShot.zoosum.util.TimeUtil;
import com.addShot.zoosum.util.exception.NotEnoughInputException;
import com.addShot.zoosum.util.exception.NotEnoughSeedException;
import com.addShot.zoosum.util.exception.NotExistAnimalException;
import com.addShot.zoosum.util.exception.NotExistIslandException;
import com.addShot.zoosum.util.exception.NotExistTreeException;
import com.addShot.zoosum.util.exception.UserNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInfoServiceImpl implements UserInfoService {

	private final UserPlogInfoRepository userPlogInfoRepository;
	private final BadgeRepository badgeRepository;
	private final UserBadgeRepository userBadgeRepository;
	private final UserAnimalRepository userAnimalRepository;
	private final AnimalMotionRepository animalMotionRepository;
	private final UserItemRepository userItemRepository;
	private final ActivityRepository activityRepository;
	private final UserRepository userRepository;

	@Override
	public MainResponse getUserMain(String userId) {

		if(userId == null) {
			throw new UserNotFoundException(CustomErrorType.USER_NOT_FOUND.getMessage());
		}

		List<UserAnimal> userAnimals = userAnimalRepository.findAllSelectedByUserId(userId).get();
		List<SelectedAnimalResponse> animalResponses = new ArrayList<>();

		if (userAnimals.size() == 0) {
			throw new NotExistAnimalException(CustomErrorType.NOT_EXIST.getMessage());
		}

		for (UserAnimal ua : userAnimals) {
			Long animalId = ua.getAnimal().getAnimalId();
			List<AnimalMotion> animalMotions = animalMotionRepository.findByAnimalId(animalId).get();
			AnimalMotion randomMotion = RandomUtil.getRandomElement(animalMotions);

			SelectedAnimalResponse response = SelectedAnimalResponse.builder()
				.animalId(animalId)
				.fileUrl(randomMotion.getFileUrl())
				.build();

			animalResponses.add(response);
		}
		//섬에 나와있는 동물 리스트 조회

		UserItem islandItem = userItemRepository.findSelectedItem(userId, ItemType.ISLAND);

		if (islandItem == null) {
			throw new NotExistIslandException(CustomErrorType.NOT_EXIST.getMessage());
		}
		//섬 테마 조회

		UserItem treeItem = userItemRepository.findSelectedItem(userId, ItemType.TREE);
		//나무 조회

		if(treeItem == null) {
			throw new NotExistTreeException(CustomErrorType.NOT_EXIST.getMessage());
		}

		MainResponse response = MainResponse.builder()
			.islandUrl(islandItem.getItem().getFileUrl())
			.treeUrl(treeItem.getItem().getFileUrl())
			.animalList(animalResponses)
			.build();

		return response;
	}

	@Override
	public MainInfoResponse getUserInfoMain(String userId) {

		if(userId == null) {
			throw new UserNotFoundException(CustomErrorType.USER_NOT_FOUND.getMessage());
		}

		UserPlogInfo userPlogInfo = userPlogInfoRepository.findById(new UserPlogInfoId(userId)).get();
		List<ActivityHistory> all = activityRepository.findAll();
		List<ActivityHistory> userActivities = activityRepository.findByUserIdAndActivityType(
			userId, ActivityType.TREE);

		Mission mission = userPlogInfo.getMission();
		double kilometer = DistanceUtil.getKilometer(mission.getMissionLength());
		System.out.println(kilometer);

		MainInfoResponse response = MainInfoResponse.builder()
			.missionLength(kilometer)
			.missionTrash(mission.getMissionTrash())
			.seed(userPlogInfo.getSeed())
			.treeAllCount(all.size())
			.treeCount(userActivities.size())
			.build();

		int missionTime = mission.getMissionTime();
		DivideTime divideTime = TimeUtil.getTime(missionTime);
		response.setHour(divideTime.getHour());
		response.setMinute(divideTime.getMinute());
		response.setSecond(divideTime.getSecond());

		return response;
	}

	@Override
	public PlogRecordResponse getPlogRecord(String nickname) {

		String userId = userRepository.findUserByNickname(nickname).getUserId();

		if(userId == null) {
			throw new UserNotFoundException(CustomErrorType.USER_NOT_FOUND.getMessage());
		}

		UserPlogInfo userPlogInfo = userPlogInfoRepository.findById(new UserPlogInfoId(userId)).get();
		double meter = userPlogInfo.getSumPloggingData().getSumLength();

		PlogRecordResponse response = PlogRecordResponse.builder()
			.plogCount(userPlogInfo.getPlogCount())
			.sumLength(DistanceUtil.getKilometer(meter))
			.sumTrash(userPlogInfo.getSumPloggingData().getSumTrash())
			.nickname(userPlogInfo.getUser().getNickname())
			.build();

		int missionTime = userPlogInfo.getSumPloggingData().getSumTime();
		DivideTime divideTime = TimeUtil.getTime(missionTime);
		response.setHour(divideTime.getHour());
		response.setMinute(divideTime.getMinute());
		response.setSecond(divideTime.getSecond());

		return response;
	}

	@Override
	public List<BadgeListItemResponse> getUserBadgeList(String nickname) {

		String userId = userRepository.findUserByNickname(nickname).getUserId();

		if(userId == null) {
			throw new UserNotFoundException(CustomErrorType.USER_NOT_FOUND.getMessage());
		}

		List<Badge> all = badgeRepository.findAll();
		List<BadgeListItemResponse> response = new ArrayList<>();

		for (Badge b : all) {
			UserBadgeId id = new UserBadgeId(userId, b.getBadgeId());
			Optional<UserBadge> badge = userBadgeRepository.findById(id);
			boolean isHave = false;
			if (badge.isPresent()) { //사용자에게 존재하는 뱃지라면
				isHave = true;
			}

			BadgeListItemResponse badgeListItemResponse = BadgeListItemResponse.builder()
				.badgeId(b.getBadgeId())
				.badgeName(b.getBadgeName())
				.fileUrl(b.getFileUrl())
				.isHave(isHave)
				.badgeCondition(b.getBadgeCondition())
				.build();

			response.add(badgeListItemResponse);
		}
		return response;
	}

	@Transactional
	@Override
	public void insertTreeCampaignData(TreeCampaignRequest request, String userId) {

		if(userId == null) {
			throw new UserNotFoundException(CustomErrorType.USER_NOT_FOUND.getMessage());
		}

		String treeName = request.getTreeName();
		String userName = request.getUserName();
		String userPhone = request.getUserPhone();
		String userBirth = request.getUserBirth();

		if(treeName == null || userName == null || userPhone == null || userBirth == null) { //작성 안한 항목이 있다면
			throw new NotEnoughInputException(CustomErrorType.UNSATISFIED_ALL_INPUT.getMessage());
		}

		//씨앗 갯수 차감
		User user = userRepository.findById(userId).get();
		UserPlogInfo userPlogInfo = userPlogInfoRepository.findById(new UserPlogInfoId(user.getUserId())).get();

		if(userPlogInfo.getSeed() < 100) { //씨앗 갯수가 100개 미만이라면
			throw new NotEnoughSeedException(CustomErrorType.UNSATISFIED_SEED_COUNT.getMessage());
		}

		userPlogInfo.setSeed(userPlogInfo.getSeed()-100);
		userPlogInfoRepository.save(userPlogInfo);

		//나무 등록
		ActivityHistory activity = ActivityHistory.builder()
			.user(user)
			.tree(new Tree(treeName, userName, userPhone, userBirth))
			.activityType(ActivityType.TREE)
			.fileUrl("추가 예정")
			.time(new Time(LocalDateTime.now(), LocalDateTime.now()))
			.build();

		activityRepository.save(activity);
	}


}
