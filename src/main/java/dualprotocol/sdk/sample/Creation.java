// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package dualprotocol.sdk.sample;

import com.azure.resourcemanager.netapp.fluent.NetAppManagementClient;
import com.azure.resourcemanager.netapp.fluent.models.CapacityPoolInner;
import com.azure.resourcemanager.netapp.fluent.models.NetAppAccountInner;
import com.azure.resourcemanager.netapp.fluent.models.VolumeInner;
import dualprotocol.sdk.sample.common.Utils;

public class Creation
{
    /**
     * Creates an ANF Account
     * @param anfClient Azure NetApp Files Management Client
     * @param accountParams Contains resource group and Account name to use
     * @param accountBody The Account body used in the creation
     * @return The newly created ANF Account
     */
    public static NetAppAccountInner createANFAccount(NetAppManagementClient anfClient, String[] accountParams, NetAppAccountInner accountBody)
    {
        try
        {
            NetAppAccountInner anfAccount = anfClient.getAccounts().beginCreateOrUpdate(accountParams[0], accountParams[1], accountBody).getFinalResult();
            Utils.writeSuccessMessage("Account successfully created, resourceId: " + anfAccount.id());

            return anfAccount;
        }
        catch (Exception e)
        {
            Utils.writeConsoleMessage("An error occurred while creating account: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a Capacity Pool
     * @param anfClient Azure NetApp Files Management Client
     * @param poolParams Contains resource group, Account name, and Pool name to use
     * @param poolBody The Capacity Pool body used in the creation
     * @return The newly created Capacity Pool
     */
    public static CapacityPoolInner createCapacityPool(NetAppManagementClient anfClient, String[] poolParams, CapacityPoolInner poolBody)
    {
        try
        {
            CapacityPoolInner capacityPool = anfClient.getPools().beginCreateOrUpdate(poolParams[0], poolParams[1], poolParams[2], poolBody).getFinalResult();
            Utils.writeSuccessMessage("Capacity Pool successfully created, resourceId: " + capacityPool.id());

            return capacityPool;
        }
        catch (Exception e)
        {
            Utils.writeConsoleMessage("An error occurred while creating capacity pool: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a Dual-Protocol Volume
     * @param anfClient Azure NetApp Files Management Client
     * @param volumeParams Contains resource group, Account name, Pool name, and Volume name to use
     * @param volumeBody The Volume body used in the creation
     * @return The newly created Volume
     */
    public static VolumeInner createVolume(NetAppManagementClient anfClient, String[] volumeParams, VolumeInner volumeBody)
    {
        try
        {
            VolumeInner volume = anfClient.getVolumes().beginCreateOrUpdate(volumeParams[0], volumeParams[1], volumeParams[2], volumeParams[3], volumeBody).getFinalResult();
            Utils.writeSuccessMessage("Volume successfully created, resourceId: " + volume.id());

            return volume;
        }
        catch (Exception e)
        {
            Utils.writeConsoleMessage("An error occurred while creating volume: " + e.getMessage());
            throw e;
        }
    }
}
