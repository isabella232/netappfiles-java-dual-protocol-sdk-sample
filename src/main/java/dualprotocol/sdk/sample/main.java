// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package dualprotocol.sdk.sample;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.netapp.NetAppFilesManager;
import com.azure.resourcemanager.netapp.fluent.models.CapacityPoolInner;
import com.azure.resourcemanager.netapp.fluent.models.NetAppAccountInner;
import com.azure.resourcemanager.netapp.fluent.models.VolumeInner;
import com.azure.resourcemanager.netapp.models.ActiveDirectory;
import com.azure.resourcemanager.netapp.models.SecurityStyle;
import com.azure.resourcemanager.netapp.models.ServiceLevel;
import dualprotocol.sdk.sample.common.CommonSdk;
import dualprotocol.sdk.sample.common.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class main
{
    /**
     * Sample console application that executes CRUD management operations on Azure NetApp Files resources
     * Showcases how to create a Dual-Protocol Volume - A Volume using both NFS and SMB protocols
     * @param args
     */
    public static void main( String[] args )
    {
        Utils.displayConsoleAppHeader();

        try
        {
            run();
            Utils.writeConsoleMessage("Sample application successfully completed execution");
        }
        catch (Exception e)
        {
            Utils.writeErrorMessage(e.getMessage());
        }

        System.exit(0);
    }

    private static void run()
    {
        //---------------------------------------------------------------------------------------------------------------------
        // Setting variables necessary for resources creation - change these to appropriate values related to your environment
        //---------------------------------------------------------------------------------------------------------------------
        boolean cleanup = false;

        String subscriptionId = "<subscription-id>";
        String location = "<location>";
        String resourceGroupName = "<resource-group-name>";
        String vnetName = "<vnet-name>";
        String subnetName = "<subnet-name>";
        String anfAccountName = "anf-java-example-account";
        String capacityPoolName = "anf-java-example-pool";
        String capacityPoolServiceLevel = "Standard"; // Valid service levels are: Ultra, Premium, Standard
        String volumeName = "anf-java-example-volume";

        long capacityPoolSize = 4398046511104L;  // 4TiB which is minimum size
        long volumeSize = 107374182400L;  // 100GiB - volume minimum size

        // SMB/CIFS related variables
        String domainJoinUsername = "testadmin";
        String dnsList = "10.0.2.4,10.0.2.5"; // Please notice that this is a comma-separated string
        String adFQDN = "testdomain.local";
        String smbServerNamePrefix = "testsmb"; // this needs to be maximum 10 characters in length and during the domain join process a random string gets appended.

        String rootCACertFullFilePath = "ad-server.cer"; // Base64 encoded root ca certificate full file name, located at root of project

        //---------------------------------------------------------------------------------------------------------------------


        // Instantiating a new ANF management client and authenticate
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        TokenCredential credential = new DefaultAzureCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                .build();
        Utils.writeConsoleMessage("Instantiating a new Azure NetApp Files management client...");
        NetAppFilesManager manager = NetAppFilesManager
                .authenticate(credential, profile);

        //------------------------------------------------------------------------------------------------------
        // Getting Active Directory Identity's password (from identity that has rights to domain join computers)
        //------------------------------------------------------------------------------------------------------
        System.out.println("Please type Active Directory's user password that will domain join ANF's SMB server and press [ENTER]:");
        String domainJoinUserPassword = Utils.getConsolePassword();

        String certContent = Utils.getRootCACert(rootCACertFullFilePath);
        if (certContent == null)
        {
            return;
        }
        String encodedCertContent = Base64.getEncoder().encodeToString(certContent.getBytes());

        //---------------------------
        // Creating ANF resources
        //---------------------------

        //---------------------------
        // Create ANF Account
        //---------------------------
        Utils.writeConsoleMessage("Creating Azure NetApp Files Account...");

        // Setting up Active Directories Object
        ActiveDirectory activeDirectory = new ActiveDirectory();
        activeDirectory.withUsername(domainJoinUsername);
        activeDirectory.withPassword(domainJoinUserPassword);
        activeDirectory.withDns(dnsList);
        activeDirectory.withDomain(adFQDN);
        activeDirectory.withSmbServerName(smbServerNamePrefix);
        activeDirectory.withServerRootCACertificate(encodedCertContent);

        NetAppAccountInner newAccount = new NetAppAccountInner();
        newAccount.withLocation(location);
        newAccount.withActiveDirectories(Collections.singletonList(activeDirectory));

        String[] accountParams = {resourceGroupName, anfAccountName};
        // Create ANF Account if it doesn't exist, update it with AD connection if it does
        NetAppAccountInner anfAccount = Creation.createANFAccount(manager.serviceClient(), accountParams, newAccount);

        //---------------------------
        // Create Capacity Pool
        //---------------------------
        Utils.writeConsoleMessage("Creating Capacity Pool...");

        CapacityPoolInner newCapacityPool = new CapacityPoolInner();
        newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevel));
        newCapacityPool.withSize(capacityPoolSize);
        newCapacityPool.withLocation(location);

        String[] poolParams = {resourceGroupName, anfAccountName, capacityPoolName};
        CapacityPoolInner capacityPool = Creation.createCapacityPool(manager.serviceClient(), poolParams, newCapacityPool);

        //---------------------------
        // Create Volume
        //---------------------------
        Utils.writeConsoleMessage("Creating Volume with dual protocol...");

        String subnetId = "/subscriptions/" + subscriptionId + "/resourceGroups/" + resourceGroupName +
                "/providers/Microsoft.Network/virtualNetworks/" + vnetName + "/subnets/" + subnetName;

        List<String> protocolTypes = new ArrayList<>();
        protocolTypes.add("CIFS");
        protocolTypes.add("NFSv3");

        VolumeInner newVolume = new VolumeInner();
        newVolume.withLocation(location);
        newVolume.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevel));
        newVolume.withCreationToken(volumeName);
        newVolume.withSubnetId(subnetId);
        newVolume.withUsageThreshold(volumeSize);
        newVolume.withProtocolTypes(protocolTypes);
        newVolume.withSecurityStyle(SecurityStyle.NTFS);

        String[] volumeParams = {resourceGroupName, anfAccountName, capacityPoolName, volumeName};
        VolumeInner volume = Creation.createVolume(manager.serviceClient(), volumeParams, newVolume);

        Utils.writeConsoleMessage("Current Volume protocol types: " + volume.protocolTypes());
        Utils.writeConsoleMessage("SMB Server FQDN: " + volume.mountTargets().get(0).smbServerFqdn());
        Utils.writeConsoleMessage("NFS IP Address: " + volume.mountTargets().get(0).ipAddress());


        //---------------------------
        // Clean up resources
        //---------------------------

        /*
          Cleanup process. For this process to take effect please change the value of
          the boolean variable 'cleanup' to 'true'
          The cleanup process starts from the innermost resources down in the hierarchy chain.
          In this case: Volume -> Capacity Pool -> Account
        */
        if (cleanup)
        {
            Utils.writeConsoleMessage("Cleaning up all created resources");

            try
            {
                Cleanup.runCleanupTask(manager.serviceClient(), volumeParams, VolumeInner.class);
                // ARM workaround to wait for the deletion to complete
                CommonSdk.waitForNoANFResource(manager.serviceClient(), volume.id(), VolumeInner.class);
                Utils.writeSuccessMessage("Volume successfully deleted: " + volume.id());

                Cleanup.runCleanupTask(manager.serviceClient(), poolParams, CapacityPoolInner.class);
                CommonSdk.waitForNoANFResource(manager.serviceClient(), capacityPool.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Primary Capacity Pool successfully deleted: " + capacityPool.id());

                Cleanup.runCleanupTask(manager.serviceClient(), accountParams, NetAppAccountInner.class);
                CommonSdk.waitForNoANFResource(manager.serviceClient(), anfAccount.id(), NetAppAccountInner.class);
                Utils.writeSuccessMessage("Account successfully deleted: " + anfAccount.id());
            }
            catch (Exception e)
            {
                Utils.writeConsoleMessage("An error occurred while deleting resource: " + e.getMessage());
                throw e;
            }
        }
    }
}
