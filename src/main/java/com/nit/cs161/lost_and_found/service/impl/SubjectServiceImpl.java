package com.nit.cs161.lost_and_found.service.impl;

import com.nit.cs161.lost_and_found.constant.EnumMessageType;
import com.nit.cs161.lost_and_found.dto.ItemDTO;
import com.nit.cs161.lost_and_found.dto.MessageDTO;
import com.nit.cs161.lost_and_found.dto.SubjectDTO;
import com.nit.cs161.lost_and_found.dto.UserDTO;
import com.nit.cs161.lost_and_found.dto.general.DtRequestDTO;
import com.nit.cs161.lost_and_found.dto.general.DtResponseDTO;
import com.nit.cs161.lost_and_found.entity.SysUser;
import com.nit.cs161.lost_and_found.entity.laf.LafItem;
import com.nit.cs161.lost_and_found.entity.laf.LafMessage;
import com.nit.cs161.lost_and_found.entity.laf.LafReturnLog;
import com.nit.cs161.lost_and_found.repository.ItemRepository;
import com.nit.cs161.lost_and_found.repository.MessageRepository;
import com.nit.cs161.lost_and_found.repository.ReturnLogRepository;
import com.nit.cs161.lost_and_found.repository.UserRepository;
import com.nit.cs161.lost_and_found.service.SubjectService;
import com.nit.cs161.lost_and_found.service.UserService;
import com.nit.cs161.lost_and_found.utility.DateGenerator;
import com.nit.cs161.lost_and_found.utility.Tools;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.*;

/**
 * Descriptions: 业务逻辑实现<p>
 *
 * @author Jin
 * @date 2018/10/6 13:53
 */
@Service
public class SubjectServiceImpl implements SubjectService {

    @Resource
    private ItemRepository itemRepository;

    @Resource
    private MessageRepository messageRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private ReturnLogRepository returnLogRepository;

    @Resource
    private UserService userService;

    // private Map<Integer, ItemDTO> itemIdMapItemDTO = new HashMap<>(50);

    @Override
    public DtResponseDTO getPageResponse(DtRequestDTO dtRequestDTO) throws Exception {
        // dtRequestDTO.start / dtRequestDTO.length 表示页数(从0开始); start表示这一页的起始元素的num-1
        Pageable pageable
                = new PageRequest(dtRequestDTO.start / dtRequestDTO.length, dtRequestDTO.length, new Sort(Sort.Direction.DESC, "createTime"));
        Page<LafMessage> page;
        String search = dtRequestDTO.getSearch();
        boolean needSearch = search != null && search != ""
                && dtRequestDTO.getMultiSearchKeyList() != null && dtRequestDTO.getMultiSearchValueList() != null;
        List<Integer> itemIdList = new LinkedList<>();
        List<LafItem> itemList = itemRepository.findAll();
        Map<Integer, LafItem> itemIdMapItem = new HashMap<>(50);
        Tools.calcKeyMapBean(itemList, itemIdMapItem, bean -> bean.getItemId());
        // 显示时以item为主 然后是message @TODO 迷
        itemList.forEach(bean -> itemIdList.add(bean.getItemId()));
        if (itemIdList.isEmpty()) {
            // -1表示一个绝对不存在的主键
            itemIdList.add(-1);
        } else {
            // DNT
        }
        Specification<LafMessage> specification = (root, criteriaQuery, criteriaBuilder) -> {
            // 过滤条件 显示所有主题(不是普通消息就视为主题)
            Predicate filter = criteriaBuilder.notEqual(root.get("messageType"), EnumMessageType.ORDINARY.getValue());
            // 搜索处理
            // 这里 multiSearchKeyList multiSearchValueList;
            // 1,2,3,4列分别表示msgTitle, messageDesc, 时间段; (若相应字段为空 则不需要对其进行搜索处理)
            if (needSearch) {
                Assert.isTrue(
                        dtRequestDTO.getMultiSearchKeyList().size() == dtRequestDTO.getMultiSearchValueList().size()
                        , "筛选数组必须不为空且长度相等"
                );
                Iterator<String> itKey = dtRequestDTO.getMultiSearchKeyList().iterator();
                Iterator<String> itValue = dtRequestDTO.getMultiSearchValueList().iterator();
                // 由于后两个字段是时间不能归一处理
                int index = -1;
                while (itKey.hasNext()) {
                    String sKey = itKey.next();
                    String sValue = itValue.next();
                    ++index;
                    if (sKey == null || sKey.equals("")) {
                        // do nothing
                    } else {
                        char order = search.charAt(index);
                        switch (order) {
                            case '0': {
                                filter = criteriaBuilder.and(filter
                                        , criteriaBuilder.like(root.get(sKey).as(String.class), "%" + sValue + "%")
                                );
                                break;
                            }
                            case '1': {
                                filter = criteriaBuilder.and(filter
                                        , criteriaBuilder.equal(root.get(sKey).as(String.class), sValue)
                                );
                                break;
                            }
                            case '3': {
                                int midIndex = sValue.indexOf("_");
                                String startDateTime = sValue.substring(0, midIndex);
                                String endDateTime = sValue.substring(midIndex + 1);
                                filter = criteriaBuilder.and(filter
                                        , criteriaBuilder.between(root.get("createTime").as(Timestamp.class)
                                                , new DateGenerator(startDateTime).toTimestamp(), new DateGenerator(endDateTime).toTimestamp())
                                );
                                break;
                            }
                        }
                    }
                }
                /*filter = criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("itemId").as(String.class), search)
                        , criteriaBuilder.like(root.get("messageDesc").as(String.class), "%" + search + "%")
                        , criteriaBuilder.like(root.get("msgTitle").as(String.class), "%" + search + "%")
                        //, criteriaBuilder.like(root.get("itemName").as(String.class), "%" + search + "%")
                        //, criteriaBuilder.like(root.get("itemDesc").as(String.class), "%" + search + "%")
                        //, criteriaBuilder.like(root.get("userUsername").as(String.class), "%" + search + "%")
                );*/
            } else {
                filter = criteriaBuilder.and(filter, root.get("itemId").in(itemIdList));
            }
            return filter;
        };
        page = messageRepository.findAll(specification, pageable);

        List<Integer> userIdList = new LinkedList<>();
        page.getContent().forEach(bean -> userIdList.add(bean.getUserId()));
        List<SysUser> userList = userRepository.findAll(userIdList);
        Map<Integer, SysUser> userIdMapUser = new HashMap<>(50);
        Tools.calcKeyMapBean(userList, userIdMapUser, bean -> bean.getUserId());

        List<SubjectDTO> pageList = new LinkedList<>();
        for (LafMessage bean : page.getContent()) {
            pageList.add(new SubjectDTO(bean, itemIdMapItem.get(bean.getItemId()), userIdMapUser.get(bean.getUserId())));
        }
        DtResponseDTO dtResponseDTO = new DtResponseDTO(dtRequestDTO.getDraw(), page.getNumberOfElements(), (int) page.getTotalElements(), pageList);
        return dtResponseDTO;
    }

    @Override
    public MessageDTO getRecord(Integer integer) throws Exception {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteRecord(Integer messageId) throws Exception {
        int delCnt = 0;
        Integer itemId = messageRepository.findOne(messageId).getItemId();
        messageRepository.delete(messageId);
        ++delCnt;
        itemRepository.delete(itemId);
        ++delCnt;
        /*if(delCnt > 0){
            throw new Exception("kua j n Exception: 只要发生异常 上述语句都不会被执行");
        }*/
        return delCnt;
    }

    @Override
    public Integer updateRecord(MessageDTO record) throws Exception {
        return null;
    }

    @Override
    public Integer saveRecord(MessageDTO record) throws Exception {
        return null;
    }

    /**
     * Descriptions: 列举一个物品的消息列表(即主题)<p>
     *
     * @author Jin
     * @date 2018/12/13 16:35
     */
    private List<MessageDTO> listItemMessage(Integer itemId) {
        List<MessageDTO> messageDTOList = new LinkedList<>();
        messageRepository.findAllByItemId(itemId).forEach(bean -> messageDTOList.add(new MessageDTO(bean)));
        return messageDTOList;
    }

    @Override
    public List<MessageDTO> listSubjectMessage(Integer messageId) throws Exception {
        List<MessageDTO> messageDTOList;
        LafMessage message = messageRepository.findOne(messageId);
        if (message == null) {
            messageDTOList = new LinkedList<>();
        } else {
            messageDTOList = listItemMessage(message.getItemId());
            List<Integer> userIdList = new LinkedList<>();
            messageDTOList.forEach(ele -> userIdList.add(ele.getUserId()));
            List<SysUser> userList = userRepository.findAll((root, query, cb) -> {
                Predicate predicate;
                predicate = cb.and(root.get("userId").in(userIdList));
                // 先写返回值有利于IDE在lambda体中快速识别重载方法
                return predicate;
            });
            Map<Integer, SysUser> userIdMapUser = new HashMap<>(50);
            Tools.calcKeyMapBean(userList, userIdMapUser, bean -> bean.getUserId());
            messageDTOList.forEach((ele) ->
                    ele.setUserUsername(userIdMapUser.get(ele.getUserId()).getUserUsername())
            );
        }
        return messageDTOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveRecord(MessageDTO record, ItemDTO itemRecord) throws Exception {
        // @TODO 多此一举的感觉
        UserDTO userDTO = userService.getRecord(record.getUserUsername());
        record.setUserId(userDTO.getUserId());
        if (record.getMessageType().equals(EnumMessageType.ORDINARY.getValue())) {
            // 普通消息: 没有标题 然后直接保存message即可
            record.setMsgTitle(null);
        } else {
            // 创建一个主题: 先创建一个item 再创建message
            itemRecord.setItemId(itemRepository.save(itemRecord.toBean()).getItemId());
            record.setItemId(itemRecord.getItemId());
        }
        return messageRepository.save(record.toBean()).getMessageId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateRecord(MessageDTO record, ItemDTO itemRecord) throws Exception {
        LafMessage msgBean = messageRepository.findOne(record.getMessageId());
        record.setCreateTime(msgBean.getCreateTime());
        record.setEditTime(new Timestamp(System.currentTimeMillis()));

        LafItem itemBean = itemRepository.findOne(msgBean.getItemId());
        itemRecord.setItemId(itemBean.getItemId());
        itemRecord.setItemPickUpTime(itemBean.getItemPickUpTime().toString());
        itemRecord.setCreateTime(msgBean.getCreateTime());
        itemRecord.setEditTime(new Timestamp(System.currentTimeMillis()));
        saveRecord(record, itemRecord);
        return 2;
    }

    @Override
    public Integer claimItem(Integer primaryKey) throws Exception {
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = httpServletRequest.getSession();
        String userName = (String) session.getAttribute("userName");
        List<SysUser> allByUserUsername = userRepository.findAllByUserUsername(userName);
        LafMessage message = messageRepository.findOne(primaryKey);
        LafItem item = itemRepository.findOne(message.getItemId());
        LafReturnLog lafReturnLog = new LafReturnLog();
        lafReturnLog.setItemId(item.getItemId());
        lafReturnLog.setPikerUserId(message.getUserId());
        lafReturnLog.setOwnerUserId(allByUserUsername.get(0).getUserId());
        lafReturnLog.setCreateTime(new Timestamp(System.currentTimeMillis()));
        lafReturnLog.setEditTime(new Timestamp(System.currentTimeMillis()));
        LafReturnLog save = returnLogRepository.save(lafReturnLog);
        // 组装消息
        LafMessage lafMessage = new LafMessage();
        lafMessage.setCreateTime(new Timestamp(System.currentTimeMillis()));
        lafMessage.setEditTime(new Timestamp(System.currentTimeMillis()));
        lafMessage.setMessageDesc("想要认领物品o(╥﹏╥)o");
        lafMessage.setItemId(item.getItemId());
        lafMessage.setMessageType((byte) 0);
        lafMessage.setUserId(allByUserUsername.get(0).getUserId());
        messageRepository.save(lafMessage);
        return save.getReturnId();
    }

    @Override
    public Integer insertRecord(MessageDTO record) throws Exception {
        return null;
    }

    @Override
    public List<SubjectType> listSubjectType() {
        List<SubjectType> subjectTypeList = new LinkedList<>();
        for (EnumMessageType e : EnumMessageType.values()) {
            subjectTypeList.add(new SubjectType(e.getValue(), e.getName()));
        }
        return subjectTypeList;
    }
}
