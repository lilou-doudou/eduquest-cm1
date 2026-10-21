package fr.eduquest.rewards;

import fr.eduquest.shared.CurrentSession;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class RewardController {
    private final RewardService rewards;

    public RewardController(RewardService rewards) {
        this.rewards = rewards;
    }

    public record EquipRequest(@Size(max = 40) String rewardCode) {}

    @GetMapping("/rewards")
    public RewardService.RewardsResponse rewards() {
        return rewards.forChild(CurrentSession.childId());
    }

    @PutMapping("/equipment/{slot}")
    public RewardService.RewardsResponse equip(@PathVariable String slot, @RequestBody EquipRequest body) {
        return rewards.equip(CurrentSession.childId(), slot, body.rewardCode());
    }
}
