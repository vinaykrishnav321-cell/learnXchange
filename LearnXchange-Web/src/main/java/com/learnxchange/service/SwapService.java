package com.learnxchange.service;

import com.learnxchange.dao.SwapRequestDAO;
import com.learnxchange.dao.UserDAO;
import com.learnxchange.dao.UserSkillDAO;
import com.learnxchange.model.SwapRequest;
import com.learnxchange.model.User;
import com.learnxchange.model.UserSkill;
import com.learnxchange.util.ServiceException;
import com.learnxchange.util.Validator;

import java.util.List;

public class SwapService {
    private final SwapRequestDAO requestDAO = new SwapRequestDAO();
    private final UserDAO userDAO = new UserDAO();
    private final UserSkillDAO userSkillDAO = new UserSkillDAO();

    public void send(User me, int receiverId, int offeredSkillId, int requestedSkillId, String message) {
        if (offeredSkillId <= 0) throw new ServiceException("Choose a skill you will teach.");
        if (requestedSkillId <= 0) throw new ServiceException("Choose a skill you want to learn.");
        String msg = Validator.require(message, "Message", 500);
        if (receiverId == me.getId()) throw new ServiceException("You cannot send a request to yourself.");

        User receiver = userDAO.findById(receiverId).orElseThrow(() -> new ServiceException("User not found."));
        if (receiver.isBlocked()) throw new ServiceException("This user is not available.");

        boolean iTeach = userSkillDAO.findByUser(me.getId()).stream()
                .anyMatch(s -> s.type() == UserSkill.Type.TEACH && s.skillId() == offeredSkillId);
        if (!iTeach) throw new ServiceException("You can only offer a skill listed under your teaching skills.");

        boolean theyTeach = userSkillDAO.findByUser(receiverId).stream()
                .anyMatch(s -> s.type() == UserSkill.Type.TEACH && s.skillId() == requestedSkillId);
        if (!theyTeach) throw new ServiceException(receiver.getName() + " does not teach the selected skill.");

        if (requestDAO.existsPending(me.getId(), receiverId, offeredSkillId, requestedSkillId))
            throw new ServiceException("You already have a pending request for this exact swap.");

        requestDAO.insert(me.getId(), receiverId, offeredSkillId, requestedSkillId, msg);
    }

    public List<SwapRequest> incoming(User me) { return requestDAO.findIncoming(me.getId()); }
    public List<SwapRequest> outgoing(User me) { return requestDAO.findOutgoing(me.getId()); }

    public long pendingIncomingCount(User me) {
        return incoming(me).stream().filter(r -> r.status() == SwapRequest.Status.PENDING).count();
    }

    public void accept(User me, int requestId) {
        SwapRequest r = load(requestId);
        requireReceiver(me, r);
        requirePending(r);
        requestDAO.updateStatus(requestId, SwapRequest.Status.ACCEPTED);
    }

    public void reject(User me, int requestId) {
        SwapRequest r = load(requestId);
        requireReceiver(me, r);
        requirePending(r);
        requestDAO.updateStatus(requestId, SwapRequest.Status.REJECTED);
    }

    public void cancel(User me, int requestId) {
        SwapRequest r = load(requestId);
        if (r.senderId() != me.getId()) throw new ServiceException("Only the sender can cancel a request.");
        requirePending(r);
        requestDAO.updateStatus(requestId, SwapRequest.Status.CANCELLED);
    }

    private SwapRequest load(int id) {
        return requestDAO.findById(id).orElseThrow(() -> new ServiceException("Request not found."));
    }

    private void requireReceiver(User me, SwapRequest r) {
        if (r.receiverId() != me.getId()) throw new ServiceException("Only the recipient can respond to this request.");
    }

    private void requirePending(SwapRequest r) {
        if (r.status() != SwapRequest.Status.PENDING)
            throw new ServiceException("This request is already " + r.status().name().toLowerCase() + ".");
    }
}
