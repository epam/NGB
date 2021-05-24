package com.epam.catgenome.manager.blast.dto;

import com.epam.catgenome.entity.blast.BlastTask;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskPage {
    List<BlastTask> blastTasks;
    long totalCount;
}
